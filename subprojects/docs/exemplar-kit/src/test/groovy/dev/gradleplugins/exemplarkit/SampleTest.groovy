package dev.gradleplugins.exemplarkit

import dev.gradleplugins.exemplarkit.fixtures.ExemplarSample
import org.hamcrest.Matchers
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.zeroturnaround.zip.ZipUtil

import static org.hamcrest.MatcherAssert.assertThat
import static org.hamcrest.Matchers.arrayContaining
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class SampleTest {
	@TempDir
	protected File testDirectory

	@Test
	void "can write empty sample to disk"() {
		Sample.empty().writeToDirectory(testDirectory)
		assertThat(testDirectory.listFiles(), Matchers.emptyArray())
	}

	@Test
	void "can write directory-base sample to disk"() {
		def sampleDirectory = writeSampleToDirectory()
		def exampleDirectory = new File(testDirectory, 'example')

		when:
		Sample.fromDirectory(sampleDirectory).writeToDirectory(exampleDirectory)

		then:
		assertThat(exampleDirectory.list(), arrayContaining('hello'))
	}

	@Test
	void "can write archive-base sample to disk"() {
		def sampleDirectory = writeSampleToDirectory()
		def exampleDirectory = new File(testDirectory, 'example')
		def archiveFile = new File(testDirectory, 'sample.zip')
		ZipUtil.pack(sampleDirectory, archiveFile)

		when:
		Sample.fromArchive(archiveFile).writeToDirectory(exampleDirectory)

		then:
		assertThat(exampleDirectory.list(), arrayContaining('hello'))
	}

	@Test
	void "restore executable permission from archive-base sample"() {
		def sampleDirectory = writeSampleToDirectory()
		def exampleDirectory = new File(testDirectory, 'example')
		def archiveFile = new File(testDirectory, 'sample.zip')
		assertEquals(0, ['zip', '-r', archiveFile.absolutePath, 'hello'].execute(null, sampleDirectory).waitFor())

		when:
		Sample.fromArchive(archiveFile).writeToDirectory(exampleDirectory)

		then:
		exampleDirectory.eachFileRecurse { println it }
		assertTrue(new File(exampleDirectory, 'hello').canExecute())
	}

	private File writeSampleToDirectory() {
		def fixture = new ExemplarSample()
		def sampleDirectory = new File(testDirectory, 'sample')
		fixture.writeToDirectory(sampleDirectory)
		new File(sampleDirectory, 'hello').executable = true
		return sampleDirectory
	}
}
