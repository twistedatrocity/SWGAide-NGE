
SWGAide prides itself with quite a good help system.
This file reads the rules to get this system to work.

Some features mentioned in this file are TODO, consider
this text a project sketch, how we want things to work.
Hence this document may be changed without further ado.



This package
============

To provide the user with documentation outside of SWGAide itself all
help files are now contained in this folder, nowhere else. The files
are obtained via the class reference to SWGAide, such as
SWGAide.class.getResource("docs/help_schematics__en.html");

TODO: this will be changed when foreign support is asked for, then
just the file name without the language part is sent as argument to
the help-viewer, which must be improved.

This container is a plain file folder, no nested folders. This is for
a few reasons: the tool that copies the files to outside the JAR file
is kept simple; we do not expect numerous files; the files are rather
organized by name; translated files are rather provided by standalone
ZIP archives.

Almost all files in this folder are copied to outside the JAR file. No
other files should be put in here. 
EXCEPTIONS: 
Files that are used during initialization of SWGAide (re/install files)
are put here. They are named "htm", without the trailing "l".
This file, a file for translators, and the English index_en.txt are put
here, for context reasons.



Files names
===========

File names must always be lower case.
See next section on how to possibly map a file name to a visible text.

File suffixes must be "html" or "png".

Examples:
help_schematics__en.html
help_schematics_laboratory_en.html
help_schematics_many_words_for_sub_feature_en.html

The first part tells the context, if it is "help", "info", or anything
else. The helper tool creates an index page grouped by this part.

The second part tells the major feature, such "schematics". The helper
tool groups the index page by this part. NOTICE the double under-score
after this part, when the tool sorts the files the major feature it is
ordered before its sub-features.

The third part tells the minor feature, for example "laboratory".
It can either be a mapped file->text value; or it is transformed so
that its capitalized counterpart reads decently. For example, the name 
help_schematics_many_words_for_sub_feature_en.html will be transformed
into the title "Many Words For Sub Feature" under the Schematics group.
If this is not acceptable you must use a mapped value.

The language part tells the language. Use the lower case version of
Java language codes. For ZIP archives in other languages this part is
used by the tool to create proper directories and index files.



The index_xx.txt
================

The file index_xx.txt, where xx is the language code maps file names
to titles on the format:

help_schematics__en.html              Schematics
help_schematics_laboratory_en.html    The Laboratory

The first white space is the delimiter, it can be one or several but
not tabs. This is a plain text file, not a word document.

Sub-features are grouped under the main feature. Hence the titles
of sub-features must not read "Schematics -- The Laboratory" but
just "The Laboratory".

For a comment, begin the line with a dash, such as
# this is a comment anywhere in the file and it is ignored
# empty lines are also OK

File names that are NOT mapped in this file are assigned a title
based on its name, see previous section.



Other languages
===============

Notice: all of the foreign language ideas are TODO.

Help files in any other language than English should be put in
packages in SVN/SWGAide/swg/docs/XX, where XX is the upper-case
language code such as DE or FR. These files will be wrapped into
separate ZIP files and published separately.

The ZIP files must be named SWGAide_help_XX.zip

The helper tool downloads and unzips such a file into docs/XX/ for
out of SWGAide browsing. In a future SWGAide will use them for the
help system as well. Then, if a certain file is missing its English
counterpart is used.

File names must be identical with their English counterparts, only
the language code is allowed to change; for example
help_schematics__en.html
help_schematics__fr.html

Include a index_xx.txt file for the mapping between file name and
the title in the created HTML index file. In SWGAide the translated
file is used as is, the help system does not use the map-file.



The helper tool
===============

XXX: probably a new class is better for this, will investigate.

This tool is rather the method swg/gui/SWGPostLaunch:docsCopy()
which copies all files with a suffix "html" and "png" to the folder
[SWGAide]\docs\XX\

While doing so it creates an index file such as
[SWGAide]\docs\index_XX.html and if a file is mapped to a title in
index_xx.txt that title is used, otherwise this tool transforms a
title based on its major or its sub-feature part.

TODO: This method supports foreign languages and scans a ZIP file
in the same manner and copies files as described above. For this it
must use a translated index_xx.txt file. It also creates a HTML index
file for the language indicated by the name of the ZIP file.

The helper tool only executes when a new version of SWGAide is found,
or if the user updates the language option, or if SWGAide finds that
the user has changed his language locale (not the date/time locale).



Greetings
Zimoon @ Chimaera
