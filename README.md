# tw2md
A small Java application to convert TiddlyWiki Repositories to Obsidian Vaults.

### Usage

```shell
java -jar tw2md.jar -h
```

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
      --add-titles          Add titles to the top of all output documents.
      --add-titles-tag=<addTitlesForTags>
                            Add titles to the top of documents when tagged with
                              this tag.
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
