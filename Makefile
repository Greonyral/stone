.PHONY: all moduleInfo sign_jars

JAVAC=@javac -g -cp $(BIN_DIR):lib -sourcepath src -d $(BIN_DIR)
BIN_DIR=classes

BRUTE_VERSION=21b
BRUTE_FILES=drum1.drummap.txt drum2.drummap.txt drum3.drummap.txt drum4.drummap.txt drum5.drummap.txt library.zip midi2abc.exe midival.exe remap.exe

MAIN_SRC_FILES=Container Event Main MasterThread ModuleLoader \
 Config OptionSetup StartupContainer Task ThreadState ModuleInfo \
 modules/Main modules/MainConfigWriter modules/Module
MAIN_SRC_DIR=util io
MAIN_SRC= \
 $(patsubst %,src/stone/%.java, $(MAIN_SRC_FILES)) \
 $(shell find $(patsubst %,src/stone/%,$(MAIN_SRC_DIR)) -name "*.java")
MAIN_CLASSES=$(patsubst src/%, $(BIN_DIR)/%, $(patsubst %.java,%.class,$(MAIN_SRC)))
MAIN_CLASSES_BASE=$(patsubst $(BIN_DIR)/%.class,%,$(MAIN_CLASSES))

ABC_SRC_FILES=AbcCreator
ABC_SRC_DIR=midiData abcCreator
ABC_SRC= \
 $(patsubst %,src/stone/modules/%.java, $(ABC_SRC_FILES)) \
 $(shell find $(patsubst %,src/stone/modules/%,$(ABC_SRC_DIR)) -name "*.java")
ABC_CLASSES=$(patsubst src/%, $(BIN_DIR)/%, $(patsubst %.java,%.class,$(ABC_SRC)))
ABC_CLASSES_BASE=$(patsubst $(BIN_DIR)/%.class,%,$(ABC_CLASSES))

SU_SRC_FILES=SongbookUpdater
#SU_SRC_DIR=
SU_SRC= \
 $(patsubst %,src/stone/modules/%.java, $(SU_SRC_FILES)) \
# $(shell find $(patsubst %,src/stone/modules/%,$(SU_SRC_DIR)) -name "*.java")
SU_CLASSES=$(patsubst src/%, $(BIN_DIR)/%, $(patsubst %.java,%.class,$(SU_SRC)))
SU_CLASSES_BASE=$(patsubst $(BIN_DIR)/%.class,%,$(SU_CLASSES))

SD_SRC_FILES=SongData
SD_SRC_DIR=songData
SD_SRC= \
 $(patsubst %,src/stone/modules/%.java, $(SD_SRC_FILES)) \
 $(shell find $(patsubst %,src/stone/modules/%,$(SD_SRC_DIR)) -name "*.java")
SD_CLASSES=$(patsubst src/%, $(BIN_DIR)/%, $(patsubst %.java,%.class,$(SD_SRC)))
SD_CLASSES_BASE=$(patsubst $(BIN_DIR)/%.class,%,$(SD_CLASSES))

BR_SRC_FILES=BruTE
#SD_SRC_DIR=songData
BR_SRC= \
 $(patsubst %,src/stone/modules/%.java, $(BR_SRC_FILES)) \
# $(shell find $(patsubst %,src/stone/modules/%,$(BR_SRC_DIR)) -name "*.java")
BR_CLASSES=$(patsubst src/%, $(BIN_DIR)/%, $(patsubst %.java,%.class,$(BR_SRC)))
BR_CLASSES_BASE=$(patsubst $(BIN_DIR)/%.class,%,$(BR_CLASSES))

FE_SRC_FILES=FileEditor
FE_SRC_DIR=fileEditor
FE_SRC= \
 $(patsubst %,src/stone/modules/%.java, $(FE_SRC_FILES)) \
 $(shell find $(patsubst %,src/stone/modules/%,$(FE_SRC_DIR)) -name "*.java")
FE_CLASSES=$(patsubst src/%, $(BIN_DIR)/%, $(patsubst %.java,%.class,$(FE_SRC)))
FE_CLASSES_BASE=$(patsubst $(BIN_DIR)/%.class,%,$(FE_CLASSES))

VC_SRC_FILES=VersionControl
VC_SRC_DIR=versionControl
VC_SRC= \
 $(patsubst %,src/stone/modules/%.java, $(VC_SRC_FILES)) \
 $(shell find $(patsubst %,src/stone/modules/%,$(VC_SRC_DIR)) -name "*.java")
VC_CLASSES=$(patsubst src/%, $(BIN_DIR)/%, $(patsubst %.java,%.class,$(VC_SRC)))
VC_CLASSES_BASE=$(patsubst $(BIN_DIR)/%.class,%,$(VC_CLASSES))

all: classes wipeJars hiddenVC normal songbook_standalone moduleInfo sign_jars

clean:
	rm -rf $(BIN_DIR)

purge:
	rm -rf $(BIN_DIR) SToNe*../build_jar modules moduleInfo brute/BruTE../build_jar brute/???

wipeJars:
	rm -rf *.jar modules/* moduleInfo/*

modules:
	mkdir -p modules

classes:
	mkdir -p classes

sign_jars:
	@./signJars.sh

brute/$(BRUTE_VERSION): brute/$(BRUTE_VERSION).zip
	unzip -d brute $<

#./build_jar-archives
modules/AbcCreator.jar: modules $(ABC_CLASSES)
	@./build_jar cfM $@ " $(patsubst $(BIN_DIR)/%,-C $(BIN_DIR) %,$(subst $$,\$$,$(shell find $(patsubst %,$(BIN_DIR)/%*.class,$(ABC_CLASSES_BASE)))))"

modules/SongbookUpdater.jar: modules $(SU_CLASSES)
	@./build_jar cfM $@ "$(patsubst $(BIN_DIR)/%,-C $(BIN_DIR) %,$(subst $$,\$$,$(shell find $(patsubst %,$(BIN_DIR)/%*.class,$(SU_CLASSES_BASE)))))"

modules/FileEditor.jar: modules $(FE_CLASSES) lib
	@./build_jar cfM $@ "$(patsubst $(BIN_DIR)/%,-C $(BIN_DIR) %,$(subst $$,\$$,$(shell find $(patsubst %,$(BIN_DIR)/%*.class,$(FE_CLASSES_BASE)))) -C lib org)"

modules/VersionControl.jar: modules $(VC_CLASSES) lib
	@./build_jar cfM $@ "$(patsubst $(BIN_DIR)/%,-C $(BIN_DIR) %,$(subst $$,\$$,$(shell find $(patsubst %,$(BIN_DIR)/%*.class,$(VC_CLASSES_BASE)))) -C lib com -C lib org)"

modules/Main.jar: modules $(MAIN_CLASSES) $(BIN_DIR)/stone/io/Icon.png
	$(JAVAC) src/stone/MasterThread.java
	@echo "url https://github.com/Greonyral/stone/raw/master/" > config.txt
	@echo "mainClass Main" >> config.txt
	@echo "modules" >> config.txt
	@echo "AbcCreator Simple GUI of BruTE" >> config.txt
	@echo "FileEditor Tools to edit abc-files" >> config.txt
	@echo "SongbookUpdater Generates the files needed for Songbook Plugin" >> config.txt
	@./build_jar cfe $@ "stone.Main $(patsubst $(BIN_DIR)/%,-C $(BIN_DIR) %,$(subst $$,\$$,$(shell find $(patsubst %,$(BIN_DIR)/%*.class,$(MAIN_CLASSES_BASE)))) $(BIN_DIR)/stone/util/UnrecognizedOSException.class $(BIN_DIR)/stone/util/UnixFileSystem.class $(BIN_DIR)/stone/util/WindowsFileSystem.class)\
        -C $(BIN_DIR) stone/io/Icon.png config.txt"

modules/Main_susa.jar: modules $(MAIN_CLASSES) $(BIN_DIR)/stone/io/Icon.png
	$(JAVAC) src/stone/MasterThread.java
	@echo "url https://github.com/Greonyral/stone/raw/master/" > config.txt
	@echo "mainClass Main_susa" >> config.txt
	@echo "modules" >> config.txt
	@echo "SongbookUpdater Generates the files needed for Songbook Plugin" >> config.txt
	@./build_jar cfe $@ "stone.Main $(patsubst $(BIN_DIR)/%,-C $(BIN_DIR) %,$(subst $$,\$$,$(shell find $(patsubst %,$(BIN_DIR)/%*.class,$(MAIN_CLASSES_BASE)))) $(BIN_DIR)/stone/util/UnrecognizedOSException.class $(BIN_DIR)/stone/util/UnixFileSystem.class $(BIN_DIR)/stone/util/WindowsFileSystem.class)\
        -C $(BIN_DIR) stone/io/Icon.png config.txt"

modules/Main_band.jar: modules $(MAIN_CLASSES) $(BIN_DIR)/stone/io/Icon.png
	$(JAVAC) src/stone/MasterThread.java
	@echo "url https://github.com/Greonyral/stone/raw/master/" > config.txt
	@echo "url_https https://github.com/Greonyral/lotro-songs.git" >> config.txt

	@echo "url_ssh git@github.com:Greonyral/lotro-songs.git" >> config.txt
	@echo "mainClass Main_band" >> config.txt
	@echo "modules" >> config.txt
	@echo "AbcCreator Simple GUI of BruTE" >> config.txt
	@echo "FileEditor Tools to edit abc-files" >> config.txt
	@echo "VersionControl Simple git-GUI to synchronize songs" >> config.txt
	@echo "SongbookUpdater Generates the files needed for Songbook Plugin" >> config.txt
	@./build_jar cfe $@ "stone.Main $(patsubst $(BIN_DIR)/%,-C $(BIN_DIR) %,$(subst $$,\$$,$(shell find $(patsubst %,$(BIN_DIR)/%*.class,$(MAIN_CLASSES_BASE)))) $(BIN_DIR)/stone/util/UnrecognizedOSException.class $(BIN_DIR)/stone/util/UnixFileSystem.class $(BIN_DIR)/stone/util/WindowsFileSystem.class)\
        -C $(BIN_DIR) stone/io/Icon.png config.txt"

#dummy modules
modules/SongData.jar: modules $(SD_CLASSES)
	@./build_jar cfM $@ "$(patsubst $(BIN_DIR)/%,-C $(BIN_DIR) %,$(subst $$,\$$,$(shell find $(patsubst %,$(BIN_DIR)/%*.class,$(SD_CLASSES_BASE)))))"

modules/BruTE.jar: modules $(BR_CLASSES) brute/$(BRUTE_VERSION)
	@./build_jar cfM $@ "$(patsubst $(BIN_DIR)/%,-C $(BIN_DIR) %,$(subst $$,\$$,$(shell find $(patsubst %,$(BIN_DIR)/%*.class,$(BR_CLASSES_BASE)))))" brute brute/$(BRUTE_VERSION) "$(BRUTE_FILES)"
	
#$(patsubst brute/$(BRUTE_VERSION)/%,-C brute/$(BRUTE_VERSION) %,\

#targets
hiddenVC: modules/Main.jar modules/AbcCreator.jar modules/SongbookUpdater.jar modules/FileEditor.jar modules/BruTE.jar modules/SongData.jar
	cp $< SToNe_hiddenVC.jar

normal: modules/Main_band.jar modules/AbcCreator.jar modules/SongbookUpdater.jar modules/VersionControl.jar modules/FileEditor.jar modules/SongData.jar modules/BruTE.jar
	cp $< SToNe.jar

songbook_standalone: modules/Main_susa.jar modules/SongbookUpdater.jar
	cp $< SongbookUpdater.jar

moduleInfo: $(BIN_DIR)/stone/updater/CreateBuilds.class
	mkdir -p moduleInfo
	java -cp $(BIN_DIR):lib stone.updater.CreateBuilds

$(BIN_DIR)/stone/io/Icon.png: Icon.png
	cp $< $@

#########################
#	classes		#
#########################
$(BIN_DIR)/stone/%.class: src/stone/%.java
	$(JAVAC) $<

$(BIN_DIR)/stone/updater/CreateBuilds.class: src/stone/updater/CreateBuilds.java $(MAIN_CLASSES_0) $(MODULE_CLASSES)
	$(JAVAC) $<


#Module classes
$(BIN_DIR)/stone/modules/Module.class: src/stone/modules/Module.java src/stone/MasterThread.java
	$(JAVAC) $^ 

$(BIN_DIR)/stone/modules/SongbookUpdater.class: src/stone/modules/SongbookUpdater.java src/stone/modules/songData/*.java $(BIN_DIR)/stone/modules/Module.class
	$(JAVAC) $< src/stone/modules/songData/*.java

$(BIN_DIR)/stone/modules/VersionControl.class: src/stone/modules/VersionControl.java src/stone/modules/versionControl/*.java $(BIN_DIR)/stone/modules/Module.class
	$(JAVAC) $< src/stone/modules/versionControl/*.java

$(BIN_DIR)/stone/modules/FileEditor.class:
	$(JAVAC) $< src/stone/modules/fileEditor/*.java

$(BIN_DIR)/stone/modules/AbcCreator.class: src/stone/modules/AbcCreator.java src/stone/modules/abcCreator/*.java src/stone/modules/midiData/*.java $(BIN_DIR)/stone/modules/Module.class
	$(JAVAC) $< src/stone/modules/abcCreator/*.java src/stone/modules/midiData/*.java

$(BIN_DIR)/stone/modules/SongData.class: src/stone/modules/SongData.java $(BIN_DIR)/stone/modules/Module.class
	$(JAVAC) $< src/stone/modules/abcCreator/*.java

#util
$(BIN_DIR)/stone/util/%.class: src/stone/util/*.java src/stone/MasterThread.java
	$(JAVAC) $^
