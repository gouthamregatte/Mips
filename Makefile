JCC = javac

server: class1 class2 class3 class4 class5 class6 class7 class8 class9 class10 class11

class1: MIPSsim.java
	$(JCC) MIPSsim.java
	
class2: Disassembler.java
	$(JCC) Disassembler.java

class3: AddRS.java
	$(JCC) AddRS.java

class4: AluRS.java
	$(JCC) AluRS.java

class5: BranchTargetBuffer.java
	$(JCC) BranchTargetBuffer.java

class6: InstFetch.java
	$(JCC) InstFetch.java

class7: InstInfo.java
	$(JCC) InstInfo.java

class8: LoadRS.java
	$(JCC) LoadRS.java

class9: LSQueue.java
	$(JCC) LSQueue.java

class10: ReorderBuffer.java
	$(JCC) ReorderBuffer.java

class11: Stages.java
	$(JCC) Stages.java

clean:
	$(RM) *.class