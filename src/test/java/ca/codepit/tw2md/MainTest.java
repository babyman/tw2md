package ca.codepit.tw2md;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * @author evan
 */
class MainTest {

	private final static URL TEST_TID = MainTest.class.getResource("tiddler.tid");

	private CommandLine cli;

	private Main main;

	@BeforeEach
	public void setup() {

		main = new Main();
		main.spaceTagCharacterReplacement = "_";
		main.illegalTagCharacterReplacement = "_";
		main.numericTagPrefix = "t";
		main.detectChecklists = true;
		main.detectChecklistHeaders = true;
		main.tagCaseConversion = Main.CASE_CONVERTER.NONE;
		main.addTitles = false;
		main.addTitlesForTags = Arrays.asList("Linux");
		cli = new CommandLine(main);
	}

	@Test
	public void checkCliHelpMessage() {

		cli.execute("-h");
	}

	@Test
	public void canReadATiddlerFile() throws URISyntaxException {

		main.readTiddler(Path.of(TEST_TID.toURI()))
						.ifPresent(t -> assertEquals(5, t.getHeaders().size()));
	}

	@Test
	public void canConvertATiddlerToMarkdownText() throws URISyntaxException, IOException {

		final String md = loadMarkdownFile("tiddler.md");
		main.readTiddler(Path.of(TEST_TID.toURI()))
						.ifPresent(t -> assertEquals(md, main.toMarkdown(t, "tiddler.tid")));
	}

	@Test
	public void convertsTiddlyWikiSyntax() throws URISyntaxException, IOException {

		final String md = loadMarkdownFile("TiddlyWiki Syntax.md");
		main.readTiddler(Path.of(getClass().getResource("TiddlyWiki Syntax.tid").toURI()))
						.ifPresent(t -> assertEquals(md, main.toMarkdown(t, "TiddlyWiki Syntax.tid")));
	}

	@Test
	public void convertsTiddlyWikiLinks() throws URISyntaxException, IOException {

		final String md = loadMarkdownFile("links.md");
		main.readTiddler(Path.of(getClass().getResource("links.tid").toURI()))
						.ifPresent(t -> assertEquals(md, main.toMarkdown(t, "links.tid")));
	}

	@Test
	public void convertsTiddlyWikiMacros() throws URISyntaxException, IOException {

		final String md = loadMarkdownFile("macros.md");
		main.readTiddler(Path.of(getClass().getResource("macros.tid").toURI()))
						.ifPresent(t -> assertEquals(md, main.toMarkdown(t, "macros.tid")));
	}

	// -------------------------------------------------------------------------------------------------------------------

	private String loadMarkdownFile(String name) throws IOException, URISyntaxException {

		return Files.readString(Paths.get(getClass().getResource(name).toURI()), StandardCharsets.UTF_8);
	}

}

