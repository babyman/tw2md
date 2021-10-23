# tw2md
A small Java application to convert [TiddlyWiki](https://tiddlywiki.com/) Repositories to [Obsidian](https://obsidian.md/)
Vaults.

## Building the code

Download the packaged executable .jar here https://github.com/babyman/tw2md/releases.

OR...

You need Java 11 and Maven to build and execute this project, if you don't have them I would recommend using
[SDKMAN](https://sdkman.io/) it's very simple to install and makes everything easier :).

Once you're set clone this repo and build away:

```shell
mvn clean package
```

The 'fat' jar `tw2md.jar` can be found in the `target` subdirectory.

## Usage

### Converting single page wiki HTML files to use TiddlyWiki server

If your TiddlyWiki isn't using TiddlyWiki server it must be converted before using `tw2md`.  This is easy to do by following these steps:

1. Download and install [Node.js](https://nodejs.org/en/download):
  - Open a “Terminal”, run `npm install -g tiddlywiki`.
    - If an error occurs, run `sudo chown -R $USER /usr/local/lib/node_modules` (that gives you admin privileges for the specific folder).
    - Run `npm install -g tiddlywiki` again.
    - Check if it works by running `tiddlywiki --version`.  If everything worked it should show you the version of TiddlyWiki.
2. Run `tiddlywiki twserver --init server`.  This will create a `twserver` directory (name of your choice) to host the tiddlywiki server files. 
3. Run `tiddlywiki twserver --listen` to start the server.

Then to convert your TiddlyWiki file into the server:
1. Open http://127.0.0.1:8080/ in your browser. It should load an empty TiddlyWiki.
2. Drag your TiddlyWiki file (e.g. "mytiddlywiki.html”) onto the top of the browser window.
3. Press “import”. This imports all of your Tiddlers into the `twserver` dircetory so we can convert them later.
4. In Terminal, press `Control + C` to exit the server.

### Running tw2md.jar

1. Check that `tw2md.jar` is installed correctly (requires Java 11) `java -jar tw2md.jar -h`
2. Convert the TiddlyWiki server files to Markdown:
  -   `java -jar tw2md.jar ./twserver ~/Documents/Obsidian/tiddlyWiki`
  -   In the above command a new `~/Documents/Obsidian/tiddlyWiki` directory will be created with converted markdown files.  You can choose any director you wish.

**TIP:** Output to an empty directory and check the generated documents, repeat till you're happy with the output then copy the files into your actual Obsidian Vault.

## Comandline Option Notes

### Rendering Tags

There are a bunch of options that apply to tag conversion because TiddlyWiki has a much more forgiving tagging system.

#### `--illegal-tag-character`

Obsidian only supports alphanumeric tags plus 3 special characters (_-/) so most symbols are illegal, this
setting lets you configure that (default is '\_').

e.g. `v1.2` => `#v1_2`

#### `--numeric-tag-prefix`

Obsidian does not support pure numeric tags, this lets you define a prefix for those tags, defaults to 't'. 

e.g. `1973` => `#t1973`

#### `--space-tag-character`

No spaces in Obsidian tags, this is the value to use instead (defaults to '-' but can be set to blank '')

e.g. `Cool Stuff` => `#Cool-Stuff`

#### `--tag-case-conversion`

Case conversion options allowed:

- PASCAL, ThisIsPascalCase
- CAMEL, thisIsCamelCase
- UPPER, tags rendered as uppercase
- LOWER, tags rendered as lowercase
- NONE, no case conversion for tag names (default)

These can be combined with the `--space-tag-character` to create a wide variety of naming styles, some examples:

- Camel case: `Cool Stuff` = --tag-case-conversion=CAMEL + --space-tag-character='' => `#coolStuff`
- Pascal case:`Cool Stuff` = --tag-case-conversion=PASCAL + --space-tag-character='' => `#CoolStuff`
- Snake case: `Cool Stuff` = --tag-case-conversion=LOWER + --space-tag-character='_' => `#cool_stuff`
- Kebab case: `Cool Stuff` = --tag-case-conversion=LOWER => `#cool-stuff`

### Checklists

TiddlyWiki does not support checklists, Obsidian does.  I often use bullet lists and then strike out items as I do them so converting
those to proper check boxed list items was helpful.

#### `--detect-checklists`

Setting this flag will render bullet/number lists as checklists if they contain a row that has been
fully struck out.

![detect_checklists.png](assets/detect_checklists.png)

#### `--detect-checklist-headers`

Adding this flag will not render checkboxes for bullet list items that have indented children.

![detect_checklists_headers.png](assets/detect_checklists_headers.png)

### Adding document titles

#### `--add-titles`

Will add the TiddlyWiki header value to the top of all documents when they are rendered.

#### `--add-titles-tag` 

Can be used to add the header to the top of documents that have been tagged with a specific tag. I use this to render tons of quotes that presented well in the TiddlyWiki UI but not so nicely in Obsidian.

e.g. `--add-titles-tag=quote`

### Organizing files

#### `--assets-path`

Organize any binary assets into a Vault subdirectory, this will include any text encoded assets saved as Tiddlers.

e.g. `--assets-path=/System/Assets`

#### `--map-tag` / `-m`

Map tags into Vault directories.

e.g. `-mJournal=Journal/Daily` will save any Journal tagged documents into the Vault under Journal/Daily. 

### Help Message

```shell
Usage: tw2md [-hV] [--add-titles] [--detect-checklist-headers]
             [--detect-checklists] [--include-system-tiddlers]
             [--assets-path=<assetPath>]
             [--illegal-tag-character=<illegalTagCharacterReplacement>]
             [--numeric-tag-prefix=<numericTagPrefix>]
             [--space-tag-character=<spaceTagCharacterReplacement>]
             [--tag-case-conversion=<tagCaseConversion>]
             [--tiddlywiki-assets-path=<tiddlyWikiAssetsPath>]
             [--add-titles-tag=<addTitlesForTags>]... [-m=<String=String>]...
             <sourceDirectory> <outputDirectory>
Convert TiddlyWiki files to Obsidian compatible markdown files.
      <sourceDirectory>     The root directory containing the tiddlyWiki
                              'tiddlers' directory.
      <outputDirectory>     The output directory were the Obsidian markdown
                              files will be saved.
      --add-titles          Add the TiddlyWiki title as a header to the top of
                              all output documents.
      --add-titles-tag=<addTitlesForTags>
                            Add the TiddlyWiki title as a header to the top of
                              documents when tagged with this tag.
      --assets-path=<assetPath>
                            Collect assets into an Obsidian vault subdirectory,
                              path is relative to the output directory.
      --detect-checklist-headers
                            Do not add checkboxes to todo list items that have
                              indented sub-items.
      --detect-checklists   Treat lists that contain a struck out item as todo
                              lists.
  -h, --help                Show this help message and exit.
      --illegal-tag-character=<illegalTagCharacterReplacement>
                            Character used to replace illegal Obsidian tag
                              characters (Default: _).
      --include-system-tiddlers
                            Include system tiddlers in the output directory.
  -m, --map-tag=<String=String>
                            Map tiddlywiki tags into Obsidian vault
                              subdirectories.
      --numeric-tag-prefix=<numericTagPrefix>
                            Prefix added in front of numeric tiddlywiki tags
                              (Default: t).
      --space-tag-character=<spaceTagCharacterReplacement>
                            Character used to replace space characters used in
                              tiddlywiki tags (Default: -).
      --tag-case-conversion=<tagCaseConversion>
                            Convert tag case, can be combined with
                              `--space-tag-character`, valid values: PASCAL,
                              CAMEL, UPPER, LOWER, NONE
      --tiddlywiki-assets-path=<tiddlyWikiAssetsPath>
                            Include assets from another directory into the
                              Obsidian vault subdirectory, path is relative to
                              the tiddlywiki home directory.
  -V, --version             Print version information and exit.
```

See also

- The Obsidian form, [Import from TiddlyWiki 5 to Obsidian](https://forum.obsidian.md/t/import-from-tiddlywiki-5-to-obsidian/731).
