SWGAide-Unity_README.txt


Installation
============

Create a new folder for SWGAide-Unity, anywhere you want.

Drag and drop the SWGAide-Unity.exe file in to the new folder.

Double click the icon for SWGAide-Unity.exe to launch it.

SWGAide guides you through its initialization. Actually, you just have to
verify (or possibly find) the folder for StarWarsGalaxies; you cannot make
a useful installation of SWGAide without SWG installed.

SWGAide creates several folders and files its directory; they are all self
explanatory.

To fully utilize SWGAide you (inventory features and resource reporting), you may wish to create
an account on https://swgaide.com/ via the forum. Please check your spam folder for the email 
verification link. Once you have verified via email, you then go into the app Options -> SWGAide
and input your forum username and password, click verify to test. Once complete you will have unlocked
all features of the app.

Note: The Pre-CU version of SWGAide cannot live in the same folder as the NGE one, please use separate installation folders.


Usage
=====

Begin to just play around; try to right click at various GUI elements.

When you have a question, press F1 at the keyboard, or select "Help". 

The Main panel's left-hand side displays
1) stations (accounts)
2) galaxies at which you have a character, or have had
3) files such as aliases, macros, and notes files

SWGAide tries to find all characters, however sometimes it can only find characters
who have saved mails. To be able to read game-mails in SWGAide you must,
in-game and for each character, execute the command,

    /mailsave

After some time a SWG system message tells how many mails that were saved.
The more mails to save the longer delay as they are retrieved on demand one by
one. Thus, retain only important mails in game, delete the rest. Once they are
saved to disk it is safe to delete in-game mails. 

NOTICE: If the system message about saved mails never shows up then there are
some old or corrupt mails which the in-game mail client cannot retrieve. In SWG,
open the mail client, select a mail one at a time beginning with the oldest. If
the mail body never shows up, delete the mail since it is corrupt at the server.
Continue until the system message shows up. Or, if you have no valuable mails,
use the command /emptyMail

NOTE: when you edit a notes files, you cannot have the very same file open
in-game while editing it outside of game. The outside edits are overwritten by
the game when you close the notepad.

Again, when you have questions about something in SWGAide, navigate to the
related panel and press F1, or select Help. Help is context sensitive. During
initiation all help files are copied to a "docs" folder as web pages.



SWGAide is Open Source
======================

Java developers and also non-programmers are warmly welcomed. 
The latter group could gather and submit all kinds of data that could be useful
in SWGAide and post that at the SWGAide forum at https://swgaide.com/ Or anything else
that is useful for other players, at any format or syntax.
Developers are welcomed to download the source code from github,
browse the code but DM me on discord before going ahead to implement stuff.



The file SWGAide.DAT
====================

This file contains all of your settings and whatever you have stored in SWGAide,
such as resource inventory, comments at schematics, etc. You cannot edit this
file, it is a "serialized Java object stream". SWGAide creates a backup of this
file regularly but also about every ten minute. If SWGAide complains that the
file is corrupt you have three options:

1) Exit SWGAide and manually replace the corrupt file with one of the recent
   backup files. This gives you a chance to copy files to a safe place, or to
   try files other than the most recent, etc.
2) Let SWGAide try the file SWGAide.BAK from the backup folder.
3) Report the issue at github




Errors and problems
===================

All reports, requests, criticism, or whatever you report is warmly welcomed:

https://github.com/twistedatrocity/SWGAide-NGE/issues

You may also use the forum for discussion of SWGAide at https://swgaide.com/



Uninstall
=========

SWGAide does not add anything to the Windows Registry and it does not spread
files all over your computer. SWGAide is contained within the folder you created
for SWGAide. Save the "mails" folder and other files which you want to keep,
then delete SWGAide's folder ... and done.
