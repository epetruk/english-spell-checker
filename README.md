# english-spell-checker

This project spell checks a .txt file uploaded by the user via a GUI. The spell check displays an additional page after the spell check completes, if spelling errors were found; this additional page displays buttons to allow one to make changes on the errors found and to save them back to the original file. The spell check is done relative to a dictionary .txt file that is also uploaded by the user. Thus, a spelling error is a word in the spell checked file that goes without a match on the dictionary file, i.e. spelling is all that is checked.

## Getting Started

1) Initiate a pull request on this repository.
2) In the pulled directory javac the src folder spellchecker, i.e. javac *.java
3) Run the spell checker, java SpellCheckMainGui.

### Prerequisites

This program requires Java 8 on the host machine.

### Execution

1) Start the gui, see "Getting Started".
2) Load dictionary file: browse for the file via the "Open Dictionary .txt file" button.
2) Load a file to spell check: browse for the file via the "Open .txt file To Spell Check" button.
3) Ensure the log panel in center of the GUI displays messages for loading the dictionary file and file to spell check.
4) Start the spell checker by clicking the "Start Spell Check" button.
5) Wait for the error detection to complete. 
6) Navigate through the errors by clicking on the Ignore, Ignore All, Change, Change All and Save buttons.


## Built With

Java 1.8


## Versioning

v1.0

## Authors

Evan Petruk

## License

This project is licensed under the GNU General Public License v3, [see license.txt](license.txt).

