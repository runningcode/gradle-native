/*
 * Copyright 2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.nokee.core.exec

import spock.lang.Specification
import spock.lang.Subject

import static dev.nokee.core.exec.CommandLineToolInvocationEnvironmentVariables.*

@Subject(CommandLineToolInvocationEnvironmentVariables)
class CommandLineToolInvocationEnvironmentVariablesTest extends Specification {
	def "can create empty environment variables"() { // verify
		expect:
		empty().asMap == [:]
		empty().asList == []
		empty().plus(from([A: 'a'])) == from([A: 'a'])
	}

	def "can merge environment variables overwriting each other"() {
		expect:
		from([A:'a']).plus(from([A: 'aa'])) == from([A: 'aa'])
	}

	def "can create environment variables from current process"() {
		expect:
		inherit().asMap == System.getenv()
		inherit().asList == System.getenv().collect { k, v -> "$k=$v" }
		inherit().plus(from([A: 'a'])) == from(System.getenv() + [A: 'a'])
	}

	def "can create environment variables from properties file"() {
		expect:
		from(propertiesFile([A: 'a', B: 'b'])) == from([A: 'a', B: 'b'])

		and:
		from(propertiesFile([A: 'a', B: 'b'])).asMap == [A: 'a', B: 'b']
		from(propertiesFile([A: 'a', B: 'b'])).asList == ['A=a', 'B=b']
		from(propertiesFile([A: 'a', B: 'b'])).plus(from([C: 'c'])) == from([A: 'a', B: 'b', C: 'c'])
	}

	static File propertiesFile(Map<String, String> values) {
		def result = File.createTempFile('test', 'properties')
		result.text = values.collect { k, v -> "$k=$v" }.join('\n')
		return result
	}
}
