/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package dev.nokee.docs.fixtures;


import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Constructs a tree of diagnostic messages.
 */
public class TreeFormatter implements DiagnosticsVisitor {
    private final StringBuilder buffer = new StringBuilder();
    private final TextOutput original;
    private Node current;

    public TreeFormatter() {
        this.original = new AbstractTextOutput() {
            @Override
            protected void doAppend(String text) {
                buffer.append(text);
            }
        };
        this.current = new Node();
    }

    @Override
    public String toString() {
        return buffer.toString();
    }

    /**
     * Starts a new node with the given text.
     */
    @Override
    public TreeFormatter node(String text) {
        if (current.state == State.TraverseChildren) {
            // First child node
            current = new Node(current, text);
        } else {
            // A sibling node
            current.state = State.Done;
            current = new Node(current.parent, text);
        }
        if (current.isTopLevelNode()) {
            // A new top level node, implicitly finish the previous node
            if (current != current.parent.firstChild) {
                // Not the first top level node
                original.append(System.lineSeparator());
            }
            original.append(text);
            current.valueWritten = true;
        }
        return this;
    }

    public void blankLine() {
        node("");
    }

    /**
     * Starts a new node with the given type name.
     */
    public TreeFormatter node(Class<?> type) {
        // Implementation is currently dumb, can be made smarter
        if (type.isInterface()) {
            node("Interface ");
        } else {
            node("Class ");
        }
        appendType(type);
        return this;
    }

    /**
     * Appends text to the current node.
     */
    public TreeFormatter append(CharSequence text) {
        if (current.state == State.CollectValue) {
            current.value.append(text);
            if (current.valueWritten) {
                original.append(text);
            }
        } else {
            throw new IllegalStateException("Cannot append text as there is no current node.");
        }
        return this;
    }

    /**
     * Appends a type name to the current node.
     */
    public TreeFormatter appendType(Type type) {
        // Implementation is currently dumb, can be made smarter
        if (type instanceof Class) {
            Class<?> classType = (Class<?>) type;
            appendOuter(classType);
            append(classType.getSimpleName());
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            appendType(parameterizedType.getRawType());
            append("<");
            Type[] typeArguments = parameterizedType.getActualTypeArguments();
            for (int i = 0; i < typeArguments.length; i++) {
                Type typeArgument = typeArguments[i];
                if (i > 0) {
                    append(", ");
                }
                appendType(typeArgument);
            }
            append(">");
        } else {
            append(type.toString());
        }
        return this;
    }

    private void appendOuter(Class<?> type) {
        Class<?> outer = type.getEnclosingClass();
        if (outer != null) {
            appendOuter(outer);
            append(outer.getSimpleName());
            append(".");
        }
    }

    /**
     * Appends an annotation name to the current node.
     */
    public TreeFormatter appendAnnotation(Class<? extends Annotation> type) {
        append("@" + type.getSimpleName());
        return this;
    }

    /**
     * Appends a method name to the current node.
     */
    public TreeFormatter appendMethod(Method method) {
        // Implementation is currently dumb, can be made smarter
        append(method.getDeclaringClass().getSimpleName());
        append(".");
        append(method.getName());
        append("()");
        return this;
    }

    /**
     * Appends some user provided value to the current node.
     */
    public TreeFormatter appendValue(@Nullable Object value) {
        // Implementation is currently dumb, can be made smarter
        if (value == null) {
            append("null");
        } else if (value.getClass().isArray()) {
            appendValues((Object[]) value);
        } else if (value instanceof String) {
            append("'");
            append(value.toString());
            append("'");
        } else {
            append(value.toString());
        }
        return this;
    }

    /**
     * Appends some user provided values to the current node.
     */
    public <T> TreeFormatter appendValues(T[] values) {
        // Implementation is currently dumb, can be made smarter
        append("[");
        for (int i = 0; i < values.length; i++) {
            T value = values[i];
            if (i > 0) {
                append(", ");
            }
            appendValue(value);
        }
        append("]");
        return this;
    }

    @Override
    public TreeFormatter startChildren() {
        if (current.state == State.CollectValue) {
            current.state = State.TraverseChildren;
        } else {
            throw new IllegalStateException("Cannot start children again");
        }
        return this;
    }

    @Override
    public TreeFormatter endChildren() {
        if (current.parent == null) {
            throw new IllegalStateException("Not visiting any node.");
        }
        if (current.state == State.CollectValue) {
            current.state = State.Done;
            current = current.parent;
        }
        if (current.state != State.TraverseChildren) {
            throw new IllegalStateException("Cannot end children.");
        }
        if (current.isTopLevelNode()) {
            writeNode(current);
        }
        current.state = State.Done;
        current = current.parent;
        return this;
    }

    private void writeNode(Node node) {
        if (node.prefix == null) {
            node.prefix = node.isTopLevelNode() ? "" : node.parent.prefix + "    ";
        }

        TextOutput output = new LinePrefixingStyledTextOutput(node.prefix);
        if (!node.valueWritten) {
            output.append(node.parent.prefix);
            output.append("  - ");
            output.append(node.value);
        }

        Separator separator = node.getFirstChildSeparator();

        if (!separator.newLine) {
            output.append(separator.text);
            Node firstChild = node.firstChild;
            output.append(firstChild.value);
            firstChild.valueWritten = true;
            firstChild.prefix = node.prefix;
            writeNode(firstChild);
        } else if (node.firstChild != null) {
            original.append(separator.text);
            writeNode(node.firstChild);
        }
        if (node.nextSibling != null) {
            original.append(System.lineSeparator());
            writeNode(node.nextSibling);
        }
    }

    private enum State {
        CollectValue, TraverseChildren, Done
    }

    private enum Separator {
        NewLine(true, System.lineSeparator()),
        Empty(false, " "),
        Colon(false, ": "),
        ColonNewLine(true, ":" + System.lineSeparator());

        Separator(boolean newLine, String text) {
            this.newLine = newLine;
            this.text = text;
        }

        final boolean newLine;
        final String text;
    }

    private static class Node {
        final Node parent;
        final StringBuilder value;
        Node firstChild;
        Node lastChild;
        Node nextSibling;
        String prefix;
        State state;
        boolean valueWritten;

        private Node() {
            this.parent = null;
            this.value = new StringBuilder();
            prefix = "";
            state = State.TraverseChildren;
        }

        private Node(Node parent, String value) {
            this.parent = parent;
            this.value = new StringBuilder(value);
            state = State.CollectValue;
            if (parent.firstChild == null) {
                parent.firstChild = this;
                parent.lastChild = this;
            } else {
                parent.lastChild.nextSibling = this;
                parent.lastChild = this;
            }
        }

        Separator getFirstChildSeparator() {
            if (firstChild == null) {
                return Separator.NewLine;
            }
            if (value.length() == 0) {
                // Always expand empty node
                return Separator.NewLine;
            }
            char trailing = value.charAt(value.length() - 1);
            if (trailing == '.') {
                // Always expand with trailing .
                return Separator.NewLine;
            }
            if (firstChild.nextSibling == null
                && firstChild.firstChild == null
                && value.length() + firstChild.value.length() < 60) {
                // A single leaf node as child and total text is not too long, collapse
                if (trailing == ':') {
                    return Separator.Empty;
                }
                return Separator.Colon;
            }
            // Otherwise, expand
            if (trailing == ':') {
                return Separator.NewLine;
            }
            return Separator.ColonNewLine;
        }

        boolean isTopLevelNode() {
            return parent.parent == null;
        }
    }

	public interface TextOutput {
		TextOutput append(CharSequence value);
	}

	private static abstract class AbstractTextOutput implements TextOutput {
		@Override
		public TextOutput append(CharSequence value) {
			String[] tokens = value.toString().split("\r?\n", -1);
			for (int i = 0; i < tokens.length - 1; ++i) {
				doAppend(tokens[i]);
				doNewline();
			}
			doAppend(tokens[tokens.length - 1]);
			return this;
		}

		protected abstract void doAppend(String text);

		protected void doNewline() {
			doAppend(System.lineSeparator());
		}
	}

	private class LinePrefixingStyledTextOutput extends AbstractTextOutput {
		private final String prefix;

		public LinePrefixingStyledTextOutput(String prefix) {
			this.prefix = prefix;
		}

		@Override
		public void doAppend(String text) {
			original.append(text);
		}

		@Override
		public void doNewline() {
			super.doNewline();
			doAppend(prefix);
		}
	}
}
