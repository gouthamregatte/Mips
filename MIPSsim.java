import java.io.*;
import java.util.*;

public class MIPSsim {
	
	private static int byteCt = 0;
	
	private static int Cycle = 1;
	
	private static long PC = 600;
	public static ArrayList<String> Instructions = new ArrayList<String>();
	private static LinkedHashMap<String,Long> Registers = new LinkedHashMap<String,Long>();
	private static ArrayList<Boolean> RegistersBusy = new ArrayList<Boolean>();	
	public static ArrayList<Long> Addresses = new ArrayList<Long>();
	private static ArrayList<String> ALUInstructions = new ArrayList<String>();
	private static ArrayList<String> ALUImmediate = new ArrayList<String>();	
	private static ArrayList<String> NoALUImmediate = new ArrayList<String>();
	private static ArrayList<String> Branchfirst = new ArrayList<String>();
	private static ArrayList<String> BranchSec = new ArrayList<String>();
	
		
	private static long IQIssueIndex = 0;
	private static Stages thisCycle = new Stages();
	private static int LoadMemoryAccess = 1;
	private static int CommitPerCycle = 1;
	private static int IssuePerCycle = 1;
	/** BTB **/
	public static BranchTargetBuffer BTB = new BranchTargetBuffer(); 
	
	/** ROB ,RS **/
	private static int ROBIdInitial = 1;
	private static ReorderBuffer ROB = new ReorderBuffer();
	private static AluRS ALUStation = new AluRS();
	private static LoadRS LoadStation = new LoadRS();
	private static AddRS AddCalcStation = new AddRS();
	private static LSQueue LSQ = new LSQueue();
	
	
	private static int AddressUnit = 1;
	
	private static Stages nextCycle = new Stages();
	
	private static InstFetch InstrFetch = new InstFetch(600);
	
	private static ArrayList<String> ByteBuffer = new ArrayList<String>();
	
	private static boolean theEnd = false; 
	
	public static ArrayList<String> OutputBuffer = new ArrayList<String>();
	
	public static LinkedHashMap<Long,String> MainMemory = new LinkedHashMap<Long,String>();
	
	public static LinkedHashMap<Long,InstInfo> InstructionMap = new LinkedHashMap<Long,InstInfo>();
	
	private static int Stime;
	private static int Etime;
	
	private static boolean Break = false;
	
	private static String InputFile = null;
	
	private static String OutputFile = null;
	
	private static String Operation = null;

	
	private static String LineString(int[] bytInt) {
		
		StringBuffer Line = new StringBuffer();
		
		int bytMask;
		
		for(int byt : bytInt){
			
			bytMask = ((int)byt & 0x00ff);
			
			for(int bit = 7; bit >= 0; bit--) {
				
				if ((bytMask & (1 << bit)) > 0)
					
					Line.append("1");
				
				else
					
					Line.append("0");
					
			}
			
		}
		
		return Line.toString();
		
	}
	
	private static void InitRegisters(){
		for (int i=0;i<32;i++) {
			Registers.put("R"+i, 0L);
			RegistersBusy.add(i, false);
		}	
	}
	
	private static int ConvertoInt(long L) {
		if (L >= 2147483647) {
			return 2147483647; 
		} else if (L <= -2147483648) {
			return -2147483648;
		} else {
			return (int) L;
		}
	}

	
	public static void main(String args[]) throws IOException {
		
		if (args.length < 2) {
			
			System.out.println("Incorrect no of arguments... \n" );			
			System.exit(1);		
		} else {			
			InputFile = args[0];			
			OutputFile = args[1];
			File f = new File(OutputFile);
			
			if (f.exists()) {
				
				f.delete();
				
			}
			if (args.length == 3) {
				if ((args[2].charAt(0) == '-') &&
					(args[2].charAt(1) == 'T')
					) {
					String trace = args[2].substring(2);
					String[] traces = trace.split(":");
					Stime = Integer.parseInt(traces[0]);
					Etime = Integer.parseInt(traces[1]);									
				} else {
					System.out.println("Third argument missing '-Tm:n'... \n" );

				}
				
			}
			
			
		}

		FileInputStream in = null;
		
		Disassembler Rp = new Disassembler();

		try {
			in = new FileInputStream(InputFile);
			
			int byt;
			
			int[] bytInt = new int[4];
			/*
			 * Read the input file and write into input string buffer.
			 */
			while ((byt = in.read()) != -1) {
				
				bytInt[byteCt] = byt;
				
				byteCt++;
				
				if (byteCt == 4) {
					
					byteCt = 0;
					
					ByteBuffer.add(LineString(bytInt));
					
				}
			}
			
			in.close();

		} catch (FileNotFoundException fnfe) {

			System.out.println("Input File does not exist...");
			
			fnfe.printStackTrace();

		} catch (IOException e) {

			System.out.println("Input File I/O exception...");
			
			e.printStackTrace();

		}
		
		while(!ByteBuffer.isEmpty()) {			
			Rp.ProcessLine(ByteBuffer.get(0));
			
			ByteBuffer.remove(0);
			
		}
		
		BufferedWriter writer = null; 
			
		try {
			writer = new BufferedWriter(new FileWriter("disoutput.txt"));
			
			int LastIndex = OutputBuffer.size() - 1;
			
			int i = 0;
			
			for(String str : OutputBuffer) {
				
				if ( i ==  LastIndex) {
					
					str = str.replaceFirst("\n", "");
					
				}
				
				writer.write(str);
				
				i++;
				
			}
			
			writer.flush();
			
			writer.close();
			
		} catch (FileNotFoundException FNE){
			
			FNE.printStackTrace();
			
		} catch (IOException IOE){
			
			IOE.printStackTrace();
			
		} catch (Exception E){
			
			E.printStackTrace();
			
		} 
		
		//System.out.println("The disassembly is saved in: disoutput.txt");
			
		/** Simulation Start **/
		InitRegisters();
		
		ALUImmediate.add("SLTI");
		ALUImmediate.add("ADDI");
		ALUImmediate.add("ADDIU");
		ALUImmediate.add("SRL");
		ALUImmediate.add("SLL");
		ALUImmediate.add("SRA");
		
		NoALUImmediate.add("SLT");
		NoALUImmediate.add("SLTU");
		NoALUImmediate.add("SUB");
		NoALUImmediate.add("SUBU");
		NoALUImmediate.add("ADD");
		NoALUImmediate.add("ADDU");
		NoALUImmediate.add("AND");
		NoALUImmediate.add("OR");
		NoALUImmediate.add("XOR");
		NoALUImmediate.add("NOR");
				
		ALUInstructions.addAll(NoALUImmediate);
		ALUInstructions.addAll(ALUImmediate);
		
		Branchfirst.add("BGTZ");
		Branchfirst.add("BGEZ");
		Branchfirst.add("BLTZ");
		Branchfirst.add("BLEZ");
		
		BranchSec.add("BEQ");
		BranchSec.add("BNE");
			
		while (!theEnd) {
						
			int f = 0;						
			long thisPC = InstrFetch.getPC();
			InstrFetch.AddInstruction(InstructionInfoGet(thisPC).getInstruction());
			String instr = InstructionInfoGet(thisPC).getInstruction() ; 
			String[] splitParts = instr.split(" ");
			
			nextCycle.Stages.add("ISSUE");
			nextCycle.StagesAddress.add(thisPC);
			boolean hitBTB = false;
			for (int i = 0; i < BTB.Address.size(); i++){
				if (BTB.Address.get(i) == thisPC) {
					hitBTB = true;
					int BTBindex = i ;
					if (BTB.PredictorOutCome.get(BTBindex).equalsIgnoreCase("Taken")) {
					InstrFetch.setPC(MIPSsim.BTB.TargetPC.get(i));
					} else {
						InstrFetch.setPC(thisPC + 4);
					}
				}
			}
			if(!hitBTB) {
				if (Branchfirst.contains(splitParts[0]) || BranchSec.contains(splitParts[0])){
			    if (BTB.Address.size() > 16) {
									BTBFree();
					} 
				BTB.Address.add(thisPC);
				long TargetPC  =0;
				if (Branchfirst.contains(splitParts[0]) ) { 
				//TargetPC = thisPC + 4 + Long.parseLong(splitParts[2].substring(1,splitParts[2].length()-1));
				  TargetPC = thisPC + 4 + Long.parseLong(splitParts[2].substring(1));
				} else if (BranchSec.contains(splitParts[0])) {
				   TargetPC = thisPC + 4 + Long.parseLong(splitParts[3].substring(1));
				}
				int BTBindex = BTB.Address.indexOf(thisPC);
				BTB.PredictorOutCome.add(BTBindex, "NotSet");
				BTB.TargetPC.add(BTBindex, TargetPC);
				}
				if (splitParts[0].equalsIgnoreCase("J")){ 
				 if (BTB.Address.size() > 16) {
									BTBFree();
					}
					BTB.Address.add(thisPC);
					long JumpAddress = Long.parseLong(splitParts[1].substring(1));
					int BTBindex = BTB.Address.indexOf(thisPC);
					BTB.PredictorOutCome.add(BTBindex, "NotSet");
					BTB.TargetPC.add(BTBindex, JumpAddress);
				}
				InstrFetch.setPC(thisPC + 4);
			}
						
			while(!thisCycle.StagesAddress.isEmpty()) {
				
				SimulateStages(thisCycle.StagesAddress.removeFirst(),thisCycle.Stages.removeFirst());
				
			}
			
			thisCycle.StagesAddress.addAll(nextCycle.StagesAddress);
			thisCycle.Stages.addAll(nextCycle.Stages);
			nextCycle.StagesAddress.clear();
			nextCycle.Stages.clear();
			CommitPerCycle = 1;
			
			AddressUnit = 1;
			IssuePerCycle = 1;
			if (Cycle >= Stime && Cycle <= Etime) {
				printfile();
			} else if (Stime == 0 && Etime == 0) {
				//break;
			}
			Cycle++;
			
		}
		
		if (Stime == 0 && Etime == 0) {
		   printfile();
		}							
	}
	
	private static void WriteToFile(ArrayList<String> str) {
		BufferedWriter writer = null; 
		
		try {
			/*
			 * Writing to ouput file.
			 */
			writer = new BufferedWriter(new FileWriter(OutputFile,true));
			
			for(String str1 : str) {
				
				writer.write(str1);
				
			}
			
			writer.flush();
			
			writer.close();
			
		} catch (FileNotFoundException FNE){
			
			FNE.printStackTrace();
			
		} catch (IOException IOE){
			
			IOE.printStackTrace();
			
		} catch (Exception E){
			
			E.printStackTrace();
			
		}
	}
	/*******  Writing into file **********/
	private static void printfile() {
		ArrayList<String> StringBuff = new ArrayList<String>();
		StringBuff.add("Cycle " + "<"+ Cycle + ">"+ ":\n");
		StringBuff.add("IQ:\n");
		int size = InstrFetch.getIQsize();
		for (int i=0;i<size;i++){
			StringBuff.add("[" +InstrFetch.getInstruction(i) +"] \n");
		}
		StringBuff.add("RS:\n");
		for(int i=0;i<ALUStation.DestID.size();i++){
			StringBuff.add("["+ ALUStation.Instruction.get(i)+"] \n");
		}
		for(int i=0;i<AddCalcStation.DestID.size();i++){
			StringBuff.add("["+ AddCalcStation.Instruction.get(i)+"] \n");
			//System.out.println("ADD:" +AddCalcStation.Instruction.get(i) ) ;
		}
		for(int i=0;i<LoadStation.DestID.size();i++){
			StringBuff.add("["+ LoadStation.Instruction.get(i)+"] \n");
			//System.out.println("LS:" +LoadStation.Instruction.get(i) ) ;
		}
		
		StringBuff.add("ROB:\n");
		
		for(int i=0;i<ROB.Id.size();i++){
			StringBuff.add(String.format("["+ ROB.Instruction.get(i)+"] \n"));
		}
		StringBuff.add("BTB:\n");
		int i=0;
		for(;i<BTB.Address.size();i++){
			StringBuff.add("[Entry " + i + "]: <" 	+ BTB.Address.get(i) + ", " 
														+ BTB.TargetPC.get(i) + ", "
														+ BTB.PredictorOutCome.get(i)
														+ ">\n");
		}	
		
		StringBuff.add("Registers:\n");
		StringBuff.add("R00:\t" 	+ Registers.get("R0") + "\t"
									+ Registers.get("R1") + "\t"
									+ Registers.get("R2") + "\t"
									+ Registers.get("R3") + "\t"
									+ Registers.get("R4") + "\t"
									+ Registers.get("R5") + "\t"
									+ Registers.get("R6") + "\t"
									+ Registers.get("R7") +"\n"
									);
		StringBuff.add("R08:\t" 	+ Registers.get("R8") + "\t"
									+ Registers.get("R9") + "\t"
									+ Registers.get("R10") + "\t"
									+ Registers.get("R11") + "\t"
									+ Registers.get("R12") + "\t"
									+ Registers.get("R13") + "\t"
									+ Registers.get("R14") + "\t"
									+ Registers.get("R15") + "\n"
									);
		StringBuff.add("R16:\t" 	+ Registers.get("R16") + "\t"
									+ Registers.get("R17") + "\t"
									+ Registers.get("R18") + "\t"
									+ Registers.get("R19") + "\t"
									+ Registers.get("R20") + "\t"
									+ Registers.get("R21") + "\t"
									+ Registers.get("R22") + "\t"
									+ Registers.get("R23") + "\n"
									);
		StringBuff.add("R24:\t" 	+ Registers.get("R24") + "\t"
									+ Registers.get("R25") + "\t"
									+ Registers.get("R26") + "\t"
									+ Registers.get("R27") + "\t"
									+ Registers.get("R28") + "\t"
									+ Registers.get("R29") + "\t"
									+ Registers.get("R30") + "\t"
									+ Registers.get("R31") + "\n"
									);
		StringBuff.add("Data Segment:\n");
		long Address = 716;
		ArrayList<String> DataSegment = new ArrayList<String>();
		for (int j=0;j<MainMemory.size();j++){
			if(MainMemory.containsKey(Address)) {
				DataSegment.add(MainMemory.get(Address));
				Address += 4;
			}
		}
		Address = 716;
		StringBuffer sb = new StringBuffer();
		sb.append(Address + ":");
		for (int j=0;j<DataSegment.size();j++) {
		
			sb.append("\t" + DataSegment.get(j));
			
		}
		StringBuff.add(sb.toString()+"\n");
		WriteToFile(StringBuff);
	}

	
	private static InstInfo InstructionInfoGet(long PC) {
		
		if (InstructionMap.containsKey(PC)) {
			return InstructionMap.get(PC);
		} else {
			InstInfo Info = new InstInfo();
			Info.setAddress(PC);
			Info.setInstruction("NOP");
			Info.setStage("IF");
			return Info;
		}
		
	}
	
	/*
	 * Check for ROB Destination Entry
	 */
	private static Long CheckROBDestination(int ROBindex, String Operand,String instruction){
		boolean GetFromRegisters = true;
		Long returnValue = null;
		if(Registers.containsKey(Operand)) {
			for (int i=0;i < ROBindex;i++) {
				if ((ROB.Destination.get(i)!=null) &&
					(ROB.Destination.get(i).equalsIgnoreCase(Operand))){
					GetFromRegisters = false;
					if(ROB.Value.get(i) != null) {
						returnValue = ROB.Value.get(i);
						break;
					}
				}
				 
			}
			if(GetFromRegisters){
				returnValue = Registers.get(Operand);
			}
			if (thisCycle.Stages.contains("WRITE RESULT")) {
				
			}
		} else {
			System.out.println("Unknown First Operand: instruction-->" + instruction);
		}
		return returnValue;
	}
	
	/** Perform stages  **/
	 
	private static void SimulateStages(long PC, String Stage){
		/*if(Stage.equalsIgnoreCase("FETCH")) {
		nextCycle.Stages.add("EXECUTE");		
		}*/
		if(Stage.equalsIgnoreCase("ISSUE")) {
			boolean onTop = false;
			String instr = InstructionInfoGet(PC).getInstruction(); 
				
			String instruction = InstrFetch.getInstruction(0);
			if(instr.equalsIgnoreCase(instruction)) {
				onTop = true;
			}
			
			if ( (IssuePerCycle == 1) && 
					(onTop)
					){
				
				String[] splitParts = instruction.split(" ");
				if ( (splitParts[0].equalsIgnoreCase("NOP")) ||
						(splitParts[0].equalsIgnoreCase("BREAK"))){
					if (ROB.Id.size() < 6) {
						ROB.Id.add(ROBIdInitial);
						int ROBindex = ROB.Id.indexOf(ROBIdInitial);
						ROB.Destination.add(ROBindex,null);
						ROB.Instruction.add(ROBindex,instruction);
						ROB.Ready.add(ROBindex,false);
						ROB.Value.add(ROBindex,null);
						nextCycle.Stages.add("COMMIT");
						nextCycle.StagesAddress.add((long)ROBIdInitial);
						if(ROBIdInitial==6) {
							ROBIdInitial = 1;
						} else {
							ROBIdInitial++;
						}
						IssuePerCycle++;
						InstrFetch.removeInstruction(0);
						
					} else {
						nextCycle.Stages.add("ISSUE");
						nextCycle.StagesAddress.add(PC);
					}
					
				}
				
				if (Branchfirst.contains(splitParts[0])){
					if (ROB.Id.size() < 6) {
						ROB.Id.add(ROBIdInitial);
						int ROBindex = ROB.Id.indexOf(ROBIdInitial);
						ROB.Destination.add(ROBindex,null);
						ROB.Instruction.add(ROBindex,instruction);
						ROB.Ready.add(ROBindex,false);
						ROB.Value.add(ROBindex,null);
						
						String FirstOperand = splitParts[1].substring(0,splitParts[1].indexOf(","));
						long TargetPC = PC + 4 + Long.parseLong(splitParts[2].substring(1));
						Long FirstValue = null;
						

						FirstValue = CheckROBDestination(ROBindex, FirstOperand,instruction);
							
						if ( (FirstValue != null) ) {
							if (ALUStation.DestID.size()<10) {
								ALUStation.DestID.add(ROBIdInitial);
								int ALUindex = ALUStation.DestID.indexOf(ROBIdInitial);
								ALUStation.Instruction.add(ALUindex,instruction);
								ALUStation.Vj.add(ALUindex,FirstOperand);
								ALUStation.Vk.add(ALUindex,null);
								ALUStation.ValueVj.add(ALUindex,null);
								ALUStation.ValueVk.add(ALUindex,null);
								ALUStation.ValueVj.set(ALUindex,FirstValue);
								
								ALUStation.ValueImm.add(ALUindex, PC);
								ALUStation.DestValue.add(ALUindex, null);
								
								nextCycle.Stages.add("EXECUTE");
								nextCycle.StagesAddress.add((long)ROBIdInitial);
								
								if(ROBIdInitial==6) {
									ROBIdInitial = 1;
								} else {
									ROBIdInitial++;
								}
								IssuePerCycle++;
								InstrFetch.removeInstruction(0);
							} else {
								FreeROB(ROBindex);
								nextCycle.Stages.add("ISSUE");
								nextCycle.StagesAddress.add(PC);
							}
													
						} else {						
							if (ALUStation.DestID.size()<10) {
								ALUStation.DestID.add(ROBIdInitial);
								int ALUindex = ALUStation.DestID.indexOf(ROBIdInitial);
								ALUStation.Instruction.add(ALUindex,instruction);
								ALUStation.Vj.add(ALUindex,FirstOperand);
								ALUStation.Vk.add(ALUindex,null);
								ALUStation.ValueVj.add(ALUindex,null);
								ALUStation.ValueVk.add(ALUindex,null);
								
								ALUStation.ValueImm.add(ALUindex, PC);
								ALUStation.DestValue.add(ALUindex, null);
								
								nextCycle.Stages.add("EXECUTE");
								nextCycle.StagesAddress.add((long)ROBIdInitial);
								
								if(ROBIdInitial==6) {
									ROBIdInitial = 1;
								} else {
									ROBIdInitial++;
								}
								IssuePerCycle++;
								InstrFetch.removeInstruction(0);
							} else {
								FreeROB(ROBindex);
								nextCycle.Stages.add("ISSUE");
								nextCycle.StagesAddress.add(PC);
							}
							
						}
						
					} else {
						//System.out.println("ROB Full for instruction ISSUE... \nAdded ISSUE to NextCycle");
						nextCycle.Stages.add("ISSUE");
						nextCycle.StagesAddress.add(PC);
					}
					
				}
				if (BranchSec.contains(splitParts[0])){
					if (ROB.Id.size() < 6) {
						ROB.Id.add(ROBIdInitial);
						int ROBindex = ROB.Id.indexOf(ROBIdInitial);
						ROB.Destination.add(ROBindex,null);
						ROB.Instruction.add(ROBindex,instruction);
						ROB.Ready.add(ROBindex,false);
						ROB.Value.add(ROBindex,null);
						
						String FirstOperand = splitParts[1].substring(0,splitParts[1].indexOf(","));
						String SecondOperand = splitParts[2].substring(0,splitParts[2].indexOf(","));
						long TargetPC = PC + 4 + Long.parseLong(splitParts[3].substring(1));
						Long FirstValue = null;
						Long SecondValue = null;

						// First Operand
						FirstValue = CheckROBDestination(ROBindex, FirstOperand,instruction);
						
						// Second Operand
						SecondValue = CheckROBDestination(ROBindex, SecondOperand,instruction);
						
						if ( (FirstValue != null) && (SecondValue != null) ) {
							if (ALUStation.DestID.size()<10) {
								ALUStation.DestID.add(ROBIdInitial);
								int ALUindex = ALUStation.DestID.indexOf(ROBIdInitial);
								ALUStation.Instruction.add(ALUindex,instruction);
								ALUStation.Vj.add(ALUindex,FirstOperand);
								ALUStation.Vk.add(ALUindex,SecondOperand);
								ALUStation.ValueVj.add(ALUindex,null);
								ALUStation.ValueVk.add(ALUindex,null);
								// Set Operands' values if at least one is present								
								ALUStation.ValueVj.set(ALUindex,FirstValue);
								ALUStation.ValueVk.set(ALUindex,SecondValue);
								
								// Send PC through ALU Station to the next stage
								ALUStation.ValueImm.add(ALUindex, PC);
								ALUStation.DestValue.add(ALUindex, null);
								
								nextCycle.Stages.add("EXECUTE");
								nextCycle.StagesAddress.add((long)ROBIdInitial);
								
								if(ROBIdInitial==6) {
									ROBIdInitial = 1;
								} else {
									ROBIdInitial++;
								}
								InstrFetch.removeInstruction(0);
								IssuePerCycle++;
							} else {
								FreeROB(ROBindex);
								nextCycle.Stages.add("ISSUE");
								nextCycle.StagesAddress.add(PC);
							}
							
							
						} else {
							/** Operands are not available **/
							
							if (ALUStation.DestID.size()<10) {
								ALUStation.DestID.add(ROBIdInitial);
								int ALUindex = ALUStation.DestID.indexOf(ROBIdInitial);
								ALUStation.Instruction.add(ALUindex,instruction);
								ALUStation.Vj.add(ALUindex,FirstOperand);
								ALUStation.Vk.add(ALUindex,SecondOperand);
								ALUStation.ValueVj.add(ALUindex,null);
								ALUStation.ValueVk.add(ALUindex,null);
								
								if (FirstValue !=null) {
									ALUStation.ValueVj.set(ALUindex,FirstValue);
								}
								if (SecondValue !=null) {
									ALUStation.ValueVk.set(ALUindex,SecondValue);
								}
						
								ALUStation.ValueImm.add(ALUindex, PC);
								ALUStation.DestValue.add(ALUindex, null);
								
								nextCycle.Stages.add("EXECUTE");
								nextCycle.StagesAddress.add((long)ROBIdInitial);
								
								if(ROBIdInitial==6) {
									ROBIdInitial = 1;
								} else {
									ROBIdInitial++;
								}
								InstrFetch.removeInstruction(0);
								IssuePerCycle++;
							} else {
								FreeROB(ROBindex);
								nextCycle.Stages.add("ISSUE");
								nextCycle.StagesAddress.add(PC);
							}
							
						}
						
					} else {
						//System.out.println("ROB Full for instruction ISSUE... \nAdded ISSUE to NextCycle");
						nextCycle.Stages.add("ISSUE");
						nextCycle.StagesAddress.add(PC);
					}
					
				}
				if (splitParts[0].equalsIgnoreCase("J")){
					if (ROB.Id.size() < 6 && ALUStation.DestID.size()<10) {
						ROB.Id.add(ROBIdInitial);
						ALUStation.DestID.add(ROBIdInitial);
						int ROBindex = ROB.Id.indexOf(ROBIdInitial);
						int ALUindex = ALUStation.DestID.indexOf(ROBIdInitial);
						
						
						ROB.Destination.add(ROBindex,null);
						ROB.Instruction.add(ROBindex,instruction);
						ROB.Ready.add(ROBindex,false);
						ROB.Value.add(ROBindex,null);
						
						ALUStation.Instruction.add(ALUindex,instruction);
						
						ALUStation.Vj.add(ALUindex,null);
						ALUStation.Vk.add(ALUindex,null);
						ALUStation.ValueVj.add(ALUindex,null);
						ALUStation.ValueVk.add(ALUindex,null);
						ALUStation.ValueImm.add(ALUindex, PC);
						ALUStation.DestValue.add(ALUindex, null);																															
						nextCycle.Stages.add("EXECUTE");
						nextCycle.StagesAddress.add((long)ROBIdInitial);
						IssuePerCycle++;
						InstrFetch.removeInstruction(0);
						if(ROBIdInitial==6) {
							ROBIdInitial = 1;
						} else {
							ROBIdInitial++;
						}
						
					} else {
						
						nextCycle.Stages.add("ISSUE");
						nextCycle.StagesAddress.add(PC);
					}
					
				}
				if (ALUImmediate.contains(splitParts[0])) {
					//ROB,ALUStation
					if ( 	(ROB.Id.size()<6) &&
							(ALUStation.DestID.size()<10) 
						) {
						//Id
						ROB.Id.add(ROBIdInitial);
						ALUStation.DestID.add(ROBIdInitial);
						
						int ROBindex = ROB.Id.indexOf(ROBIdInitial); 
						int ALUindex = ALUStation.DestID.indexOf(ROBIdInitial);
						
						String ROBDestination = splitParts[1].substring(0,splitParts[1].indexOf(","));
						String FirstOperand = splitParts[2].substring(0,splitParts[2].indexOf(","));
						String SecondOperand = splitParts[3];
						// Destination, Instruction, Value, Ready
						ROB.Destination.add(ROBindex,null);
						ROB.Instruction.add(ROBindex,instruction);
						ROB.Ready.add(ROBindex,false);
						ROB.Value.add(ROBindex,null);
						// ALU station - Allocate 
						ALUStation.Instruction.add(ALUindex,instruction);
						ALUStation.Vj.add(ALUindex,FirstOperand);
						ALUStation.Vk.add(ALUindex,null);
						ALUStation.ValueVj.add(ALUindex,null);
						ALUStation.ValueVk.add(ALUindex,null);
						ALUStation.ValueImm.add(ALUindex, Long.parseLong(SecondOperand.substring(1)));
						ALUStation.DestValue.add(ALUindex, null);
						
						ALUStation.ValueVj.set(ALUindex,CheckROBDestination(ROBindex, FirstOperand,instruction));
						ROB.Destination.set(ROBindex,ROBDestination);
						nextCycle.StagesAddress.add((long)ROBIdInitial);
						nextCycle.Stages.add("EXECUTE");
						if(ROBIdInitial==6) {
							ROBIdInitial = 1;
						} else {
							ROBIdInitial++;
						}
						InstrFetch.removeInstruction(0);
						IssuePerCycle++;
					} else {
						
						nextCycle.Stages.add("ISSUE");
						nextCycle.StagesAddress.add(PC);
					}				
				}
				if (NoALUImmediate.contains(splitParts[0])) {
					if ( 	(ROB.Id.size()<6) &&
							(ALUStation.DestID.size()<10) 
						) {
						ROB.Id.add(ROBIdInitial);
						ALUStation.DestID.add(ROBIdInitial);
						
						int ROBindex = ROB.Id.indexOf(ROBIdInitial); 
						int ALUindex = ALUStation.DestID.indexOf(ROBIdInitial);
						
						String ROBDestination = splitParts[1].substring(0,splitParts[1].indexOf(","));
						String FirstOperand = splitParts[2].substring(0,splitParts[2].indexOf(","));
						String SecondOperand = splitParts[3];
						
						// Destination, Instruction, Value, Ready
						ROB.Destination.add(ROBindex,null);
						ROB.Instruction.add(ROBindex,instruction);
						ROB.Ready.add(ROBindex,false);
						ROB.Value.add(ROBindex,null);
						// ALU station - Allocate 
						ALUStation.Instruction.add(ALUindex,instruction);
						ALUStation.Vj.add(ALUindex,FirstOperand);
						ALUStation.Vk.add(ALUindex,SecondOperand);
						ALUStation.ValueVj.add(ALUindex,null);
						ALUStation.ValueVk.add(ALUindex,null);
						ALUStation.ValueImm.add(ALUindex, null);
						ALUStation.DestValue.add(ALUindex, null);
						
						ALUStation.ValueVj.set(ALUindex,CheckROBDestination(ROBindex, FirstOperand,instruction));
						ALUStation.ValueVk.set(ALUindex,CheckROBDestination(ROBindex, SecondOperand,instruction));
						ROB.Destination.set(ROBindex,ROBDestination);
						nextCycle.StagesAddress.add((long)ROBIdInitial);
						nextCycle.Stages.add("EXECUTE");
						if(ROBIdInitial==6) {
							ROBIdInitial = 1;
						} else {
							ROBIdInitial++;
						}
						InstrFetch.removeInstruction(0);
						IssuePerCycle++;
						
					} else {
						//System.out.println("ROB,ALUStation,etc. Full for instruction ISSUE... \nAdded ISSUE to NextCycle");
						nextCycle.Stages.add("ISSUE");
						nextCycle.StagesAddress.add(PC);
					}				
				}
				if(instruction.substring(0, 2).equalsIgnoreCase("LW")) {
					if ( 	(ROB.Id.size()<6) &&
							((LoadStation.DestID.size()+ALUStation.DestID.size())<10) &&
							((AddCalcStation.DestID.size()+ALUStation.DestID.size())<10) &&
							(LSQ.DestID.size()<10)
						) {
						ROB.Id.add(ROBIdInitial);
						LoadStation.DestID.add(ROBIdInitial);
						AddCalcStation.DestID.add(ROBIdInitial);
						LSQ.DestID.add(ROBIdInitial);
						
						int ROBindex = ROB.Id.indexOf(ROBIdInitial); 
						int LSindex = LoadStation.DestID.indexOf(ROBIdInitial);
						int ACRSindex = AddCalcStation.DestID.indexOf(ROBIdInitial);
						int LSQindex = LSQ.DestID.indexOf(ROBIdInitial);
						
						String ROBDestination = instruction.substring(3,instruction.indexOf(",")); 
						ROB.Destination.add(ROBindex,null);
						ROB.Instruction.add(ROBindex,instruction);
						ROB.Ready.add(ROBindex,false);
						ROB.Value.add(ROBindex,null);
					
						LoadStation.Instruction.add(LSindex,instruction);
						LoadStation.Operand.add(LSindex,ROBDestination);
						LoadStation.Value.add(LSindex,null);

						String base = instruction.substring(instruction.indexOf("(")+1,instruction.indexOf(")"));
						AddCalcStation.base.add(ACRSindex,base);
						//AddCalcStation.Instruction.add(instruction);
						String offset = instruction.substring(instruction.indexOf(",")+2,instruction.indexOf("("));
						AddCalcStation.offset.add(ACRSindex,Long.parseLong(offset));
						AddCalcStation.Value.add(ACRSindex,null);
						
						AddCalcStation.Value.set(ACRSindex,CheckROBDestination(ROBindex,base,instruction));
						ROB.Destination.set(ROBindex,ROBDestination);
						
						LSQ.Address.add(LSQindex,null);
						LSQ.value.add(LSQindex,null);
						LSQ.Instruction.add(LSQindex,instruction);
						LSQ.ExecState.add(LSQindex,"FIRST CYCLE");
						
						nextCycle.StagesAddress.add((long)ROBIdInitial);
						nextCycle.Stages.add("EXECUTE");
						if(ROBIdInitial==6) {
							ROBIdInitial = 1;
						} else {
							ROBIdInitial++;
						}
						InstrFetch.removeInstruction(0);
						IssuePerCycle++;
						
					} else {
						
						nextCycle.Stages.add("ISSUE");
						nextCycle.StagesAddress.add(PC);
					}
				}
				
				if(instruction.substring(0, 2).equalsIgnoreCase("SW")) {
					//ROB,AddCalcStation,LSQ
					if ( 	(ROB.Id.size()<6) &&
							((AddCalcStation.DestID.size() + ALUStation.DestID.size())<10) &&
							(LSQ.DestID.size()<10)
						) {
						ROB.Id.add(ROBIdInitial);
						AddCalcStation.DestID.add(ROBIdInitial);
						AddCalcStation.Instruction.add(instruction);
						LSQ.DestID.add(ROBIdInitial);
						
						int ROBindex = ROB.Id.indexOf(ROBIdInitial); 
						int ACRSindex = AddCalcStation.DestID.indexOf(ROBIdInitial);
						int LSQindex = LSQ.DestID.indexOf(ROBIdInitial);
						
						
						ROB.Destination.add(ROBindex,null);
						ROB.Instruction.add(ROBindex,instruction);
						ROB.Ready.add(ROBindex,false);
						ROB.Value.add(ROBindex,null);
						String StoreSourceRegister = splitParts[1].substring(0, splitParts[1].length()-1);
						// Allocate AC RS
						String base = instruction.substring(instruction.indexOf("(")+1,instruction.indexOf(")"));
						AddCalcStation.base.add(ACRSindex,base);
						String offset = instruction.substring(instruction.indexOf(",")+2,instruction.indexOf("("));
						AddCalcStation.offset.add(ACRSindex,Long.parseLong(offset));
						AddCalcStation.Value.add(ACRSindex,CheckROBDestination(ROBindex, base, instruction));
						
						
						LSQ.Address.add(LSQindex,null);
						LSQ.value.add(LSQindex,CheckROBDestination(ROBindex, StoreSourceRegister, instruction));
						LSQ.Instruction.add(LSQindex,instruction);
						LSQ.ExecState.add(LSQindex,"FIRST CYCLE");
						
						nextCycle.StagesAddress.add((long)ROBIdInitial);
						nextCycle.Stages.add("EXECUTE");
						if(ROBIdInitial==6) {
							ROBIdInitial = 1;
						} else {
							ROBIdInitial++;
						}
						
						InstrFetch.removeInstruction(0);
						IssuePerCycle++;
					} else {
						
						nextCycle.Stages.add("ISSUE");
						nextCycle.StagesAddress.add(PC);
					}
				}
				
			} else {
				
				nextCycle.Stages.add("ISSUE");
				nextCycle.StagesAddress.add(PC);
				
			}
			
		}
		
		if(Stage.equalsIgnoreCase("EXECUTE")) {
			
			int ROBId = (int) PC;
			String instruction = ROB.Instruction.get(ROB.Id.indexOf(ROBId));
			String[] splitParts = instruction.split(" ");
			if (Branchfirst.contains(splitParts[0])){
				int ALUindex = ALUStation.DestID.indexOf(ROBId);
				int ROBindex = ROB.Id.indexOf(ROBId);
				if ( (ALUStation.ValueVj.get(ALUindex) !=null) ) {
					//Can be resolved here; Next Go to commit stage.
					nextCycle.Stages.add("COMMIT");
					nextCycle.StagesAddress.add(PC);
					
					long FirstValue = ALUStation.ValueVj.get(ALUindex);
					long ProgCounter = ALUStation.ValueImm.get(ALUindex);
					long TargetPC = ProgCounter + 4 + Long.parseLong(splitParts[2].substring(1));
					
					boolean result = false;
					if (splitParts[0].equalsIgnoreCase("BLTZ")) {
						if (FirstValue < 0){
							result = true;
						}
					}
					if (splitParts[0].equalsIgnoreCase("BGTZ")) {
						if (FirstValue > 0){
							result = true;
						}
					}
					if (splitParts[0].equalsIgnoreCase("BLEZ")) {
						if (FirstValue <= 0){
							result = true;
						}
					}
					if (splitParts[0].equalsIgnoreCase("BGEZ")) {
						if (FirstValue >= 0){
							result = true;
						}
					}
					//FreeALUStation(ALUindex);
					
					boolean BTBBranchHit = false;
					int BTBindex = -1; 
					for (int i = 0; i < BTB.Address.size(); i++){
						if (BTB.Address.get(i) == ProgCounter) {
							BTBBranchHit = true;
							BTBindex = i;
						}
					}
					if(!BTBBranchHit) {
					    System.out.println("Something went wrong ") ;						
					} else {
						assert(BTBindex != -1);
						// If Branch Hit
						
						if (result){
							if (BTB.PredictorOutCome.get(BTBindex).equalsIgnoreCase("Not Taken") || BTB.PredictorOutCome.get(BTBindex).equalsIgnoreCase("NotSet")) {
								// misprediction
								MisPredictedFlush(ROBindex);
								InstrFetch.CompleteFlush();
								
								InstrFetch.setPC(TargetPC);
							
							}
							BTB.PredictorOutCome.set(BTBindex, "Taken");
								
							
						} else {
							// see whether prediction was correct...
							if (BTB.PredictorOutCome.get(BTBindex).equalsIgnoreCase("Taken")) {
								// misprediction
								MisPredictedFlush(ROBindex);
								InstrFetch.CompleteFlush();
								
								InstrFetch.setPC(TargetPC);
							}
							BTB.PredictorOutCome.set(BTBindex, "Not Taken");
						}
						BTB.TargetPC.set(BTBindex, TargetPC);
					}
					
				} else {
					nextCycle.StagesAddress.add(PC);
					nextCycle.Stages.add("EXECUTE");
				}
			}
			
			if (splitParts[0].equalsIgnoreCase("J")){
						//System.out.println(" IN jump") ;
			          
						int ALUindex = ALUStation.DestID.indexOf(ROBId);
						int ROBindex = ROB.Id.indexOf(ROBId);
						long ProgCounter = ALUStation.ValueImm.get(ALUindex);
						boolean BTBJumpHit = false;
						int JBTBindex = -1; 
						for (int i = 0; i < BTB.Address.size(); i++){
							if (BTB.Address.get(i) == ProgCounter) {
								BTBJumpHit = true;
								//System.out.println("Branch hit true" + i) ;
								JBTBindex = i;
							}
						}
						if(!BTBJumpHit){
						//System.out.println("jump not hit ? why ? " ) ;
							if (BTB.Address.size() > 16) {
								BTBFree();
							} 
							long JumpAddress = Long.parseLong(splitParts[1].substring(1));
							BTB.Address.add(PC);
							int BTBindex = BTB.Address.indexOf(PC);							
							BTB.PredictorOutCome.add(BTBindex, "Taken");
							BTB.TargetPC.add(BTBindex, JumpAddress);
					
							 MisPredictedFlush(ROBindex);
							InstrFetch.CompleteFlush();
							for (int j = 0; j<thisCycle.StagesAddress.size();j++) {
								if (thisCycle.Stages.get(j).equalsIgnoreCase("ISSUE")){
									thisCycle.StagesAddress.remove(j);
									thisCycle.Stages.remove(j);
									j--;
								}
							}
							for (int j = 0; j<nextCycle.StagesAddress.size();j++) {
								if (nextCycle.Stages.get(j).equalsIgnoreCase("ISSUE")){
									nextCycle.StagesAddress.remove(j);
									nextCycle.Stages.remove(j);
									j--;
								}
							}
							nextCycle.StagesAddress.clear();
							nextCycle.Stages.clear();
						
							InstrFetch.setPC(JumpAddress);
							
						} else if (BTBJumpHit && BTB.PredictorOutCome.get(JBTBindex).equalsIgnoreCase("NotSet")){
						//System.out.println("jump  hit  " ) ;
								BTB.PredictorOutCome.add(JBTBindex, "Taken");
								long JumpAddress = Long.parseLong(splitParts[1].substring(1));
								BTB.TargetPC.add(JBTBindex, JumpAddress);
								MisPredictedFlush(ROBindex);
								InstrFetch.CompleteFlush();
								/*for (int j = 0; j<thisCycle.StagesAddress.size();j++) {
								if (thisCycle.Stages.get(j).equalsIgnoreCase("ISSUE")){
									thisCycle.StagesAddress.remove(j);
									thisCycle.Stages.remove(j);
									j--;
								}
							}
							for (int j = 0; j<nextCycle.StagesAddress.size();j++) {
								if (nextCycle.Stages.get(j).equalsIgnoreCase("ISSUE")){
									nextCycle.StagesAddress.remove(j);
									nextCycle.Stages.remove(j);
									j--;
								}
							}*/
							nextCycle.StagesAddress.clear();
							nextCycle.Stages.clear();
							InstrFetch.setPC(JumpAddress);
							
						} else {
						 //System.out.println("" ) ;
						   
						}
						
						//FreeALUStation(ALUindex);
						// Next Cycle- Commit this Jump instruction
						nextCycle.Stages.add("COMMIT");
						nextCycle.StagesAddress.add(PC);				
			}
			if (BranchSec.contains(splitParts[0])){
				int ALUindex = ALUStation.DestID.indexOf(ROBId);
				int ROBindex = ROB.Id.indexOf(ROBId);
				if ( (ALUStation.ValueVj.get(ALUindex) !=null) && (ALUStation.ValueVk.get(ALUindex) !=null) ) {
					nextCycle.Stages.add("COMMIT");
					nextCycle.StagesAddress.add(PC);
										
					long ProgCounter = ALUStation.ValueImm.get(ALUindex);
					long TargetPC = ProgCounter + 4 + Long.parseLong(splitParts[3].substring(1));
					
					boolean result = false;
					if (splitParts[0].equalsIgnoreCase("BEQ")) {
						if (ALUStation.ValueVj.get(ALUindex) == ALUStation.ValueVk.get(ALUindex)){
							result = true;
						}
					}
					if (splitParts[0].equalsIgnoreCase("BNE")) {
						if (ALUStation.ValueVj.get(ALUindex) != ALUStation.ValueVk.get(ALUindex)){
							result = true;
						}
					}
					boolean BTBBranchHit = false;
					int BTBindex = -1; 
					for (int i = 0; i < BTB.Address.size(); i++){
						if (BTB.Address.get(i) == ProgCounter) {
							BTBBranchHit = true;
							BTBindex = i;
						}
					}
					//FreeALUStation(ALUindex);
					if(!BTBBranchHit) {
					    System.out.println("something went wrong ") ;	
					} else {
						
						assert(BTBindex != -1);
						
						if (result){
							// see whether prediction was correct...
							if (BTB.PredictorOutCome.get(BTBindex).equalsIgnoreCase("Not Taken") || BTB.PredictorOutCome.get(BTBindex).equalsIgnoreCase("NotSet")) {
								// misprediction
								
								MisPredictedFlush(ROBindex);
								InstrFetch.CompleteFlush();								
								InstrFetch.setPC(TargetPC);
							}
							BTB.PredictorOutCome.set(BTBindex, "Taken");											
							
						} else {
							if (BTB.PredictorOutCome.get(BTBindex).equalsIgnoreCase("Taken")) {
								// misprediction
								MisPredictedFlush(ROBindex);
								InstrFetch.CompleteFlush();
								InstrFetch.setPC(TargetPC);								
							}
							BTB.PredictorOutCome.set(BTBindex, "Not Taken");
								
						}
						BTB.TargetPC.set(BTBindex, TargetPC);
					}
					
				} else {
					nextCycle.StagesAddress.add(PC);
					nextCycle.Stages.add("EXECUTE");
				}
			}
			
			if (ALUImmediate.contains(splitParts[0])) {
				int ALUindex = ALUStation.DestID.indexOf(ROBId);
				int ROBindex = ROB.Id.indexOf(ROBId);
				if ( ALUStation.ValueVj.get(ALUindex) !=null)   {
					//SLTI
					if (splitParts[0].equalsIgnoreCase("SLTI")) {
						long result = 0; 
						if (ALUStation.ValueVj.get(ALUindex) < ALUStation.ValueImm.get(ALUindex)) {
							result = 1;
						}
						ALUStation.DestValue.set(ALUindex, result); 
					}
					//ADDI
					if (splitParts[0].equalsIgnoreCase("ADDI")) {
						long result = ALUStation.ValueVj.get(ALUindex) + ALUStation.ValueImm.get(ALUindex);
						if ( (result < -2147483648) || (result > 2147483647)){
							System.out.println("IntegerOverFlow exception for instruction: " + instruction);
							System.exit(1);
						}
						ALUStation.DestValue.set(ALUindex, result); 
					}
					//ADDIU
					if (splitParts[0].equalsIgnoreCase("ADDIU")) {
						long result = ALUStation.ValueVj.get(ALUindex) + ALUStation.ValueImm.get(ALUindex);
						if (result < -2147483648){
							result = -2147483648;
						} else if (result > 2147483647) {
							result = 2147483647;
						}
						ALUStation.DestValue.set(ALUindex,result); 
					}
					//SRL : 
					if (splitParts[0].equalsIgnoreCase("SRL")) {
						int rt = ConvertoInt(ALUStation.ValueVj.get(ALUindex));
						int sa = ConvertoInt(ALUStation.ValueImm.get(ALUindex));
						long result = rt >>> sa;
						ALUStation.DestValue.set(ALUindex, result); 
					}
					//SLL
					if (splitParts[0].equalsIgnoreCase("SLL")) {
						int rt = ConvertoInt(ALUStation.ValueVj.get(ALUindex));
						int sa = ConvertoInt(ALUStation.ValueImm.get(ALUindex));
						long result = rt << sa;
						ALUStation.DestValue.set(ALUindex, result); 
					}
					//SRA
					if (splitParts[0].equalsIgnoreCase("SRA")) {
						int rt = ConvertoInt(ALUStation.ValueVj.get(ALUindex));
						int sa = ConvertoInt(ALUStation.ValueImm.get(ALUindex));
						long result = rt >> sa;
						ALUStation.DestValue.set(ALUindex, result); 
					}
					// execution complete ...
					nextCycle.StagesAddress.add(PC);
					nextCycle.Stages.add("WRITE RESULT");
				} else {
					nextCycle.StagesAddress.add(PC);
					nextCycle.Stages.add("EXECUTE");
				}
			}
			if (NoALUImmediate.contains(splitParts[0])) {
				int ALUindex = ALUStation.DestID.indexOf(ROBId);
				int ROBindex = ROB.Id.indexOf(ROBId);
				if ( (ALUStation.ValueVj.get(ALUindex) !=null) && (ALUStation.ValueVk.get(ALUindex) !=null) ) {					
					if (splitParts[0].equalsIgnoreCase("AND")) {          //AND
						long result =  ( ConvertoInt(ALUStation.ValueVj.get(ALUindex)) &
										ConvertoInt(ALUStation.ValueVk.get(ALUindex)) );
						ALUStation.DestValue.set(ALUindex, result); 
					}
					if (splitParts[0].equalsIgnoreCase("OR")) {          //OR
						long result =  ( ConvertoInt(ALUStation.ValueVj.get(ALUindex)) |
										ConvertoInt(ALUStation.ValueVk.get(ALUindex)) );
						ALUStation.DestValue.set(ALUindex, result); 
					}
					if (splitParts[0].equalsIgnoreCase("XOR")) {        //XOR
						long result =  ( ConvertoInt(ALUStation.ValueVj.get(ALUindex)) ^
										ConvertoInt(ALUStation.ValueVk.get(ALUindex)) );
						ALUStation.DestValue.set(ALUindex, result); 
					}
					if (splitParts[0].equalsIgnoreCase("NOR")) {      //NOR
						long result =  ( ~(	ConvertoInt(ALUStation.ValueVj.get(ALUindex)) |
											ConvertoInt(ALUStation.ValueVk.get(ALUindex))	) );
						ALUStation.DestValue.set(ALUindex, result); 
					}
					
					if (splitParts[0].equalsIgnoreCase("ADD")) {    //ADD
						long result = ALUStation.ValueVj.get(ALUindex) + ALUStation.ValueVk.get(ALUindex);
						if ( (result < -2147483648) || (result > 2147483647)){
							System.out.println("IntegerOverFlow exception for instruction: " + instruction);
							System.exit(1);
						}
						ALUStation.DestValue.set(ALUindex, result); 
					}
					if (splitParts[0].equalsIgnoreCase("ADDU")) {   //ADDU
						long result = ALUStation.ValueVj.get(ALUindex) + ALUStation.ValueVk.get(ALUindex);
						if (result < -2147483648){
							result = -2147483648;
						} else if (result > 2147483647) {
							result = 2147483647;
						}
						ALUStation.DestValue.set(ALUindex,result); 
					}
					if (splitParts[0].equalsIgnoreCase("SUB")) {   //SUB
						long result = ALUStation.ValueVj.get(ALUindex) - ALUStation.ValueVk.get(ALUindex);
						if ( (result < -2147483648) || (result > 2147483647)){
							System.out.println("IntegerOverFlow exception for instruction: " + instruction);
							System.exit(1);
						}
						ALUStation.DestValue.set(ALUindex, result); 
					}
					if (splitParts[0].equalsIgnoreCase("SUBU")) {   //SUBU
						long result = ALUStation.ValueVj.get(ALUindex) - ALUStation.ValueVk.get(ALUindex);
						if (result < -2147483648){
							result = -2147483648;
						} else if (result > 2147483647) {
							result = 2147483647;
						}
						ALUStation.DestValue.set(ALUindex, result); 
					}
					
					if (splitParts[0].equalsIgnoreCase("SLT")) {     //SLT
						long result = 0; 
						if (ALUStation.ValueVj.get(ALUindex) < ALUStation.ValueVk.get(ALUindex)) {
							result = 1;
						}
						ALUStation.DestValue.set(ALUindex, result); 
					}
					if (splitParts[0].equalsIgnoreCase("SLTU")) {     //SLTU
						long result = 0; 
						if (Math.abs(ALUStation.ValueVj.get(ALUindex)) < Math.abs(ALUStation.ValueVk.get(ALUindex))) {
							result = 1;
						}
						ALUStation.DestValue.set(ALUindex, result); 
					}
					nextCycle.StagesAddress.add(PC);
					nextCycle.Stages.add("WRITE RESULT");
				} else {
					nextCycle.StagesAddress.add(PC);
					nextCycle.Stages.add("EXECUTE");
				}
			}
			if(instruction.substring(0, 2).equalsIgnoreCase("LW")) {
				int LSQindex = LSQ.DestID.indexOf(ROBId);
				int ACRSindex = AddCalcStation.DestID.indexOf(ROBId);
				int LSindex = LoadStation.DestID.indexOf(ROBId);
				if (LSQ.ExecState.get(LSQindex).equalsIgnoreCase("FIRST CYCLE")) {
					if ( (AddressUnit == 1) || (AddressUnit == 2) ) {
						if(AddCalcStation.Value.get(ACRSindex)!=null){
							long memoryLocation = (long)AddCalcStation.Value.get(ACRSindex) + AddCalcStation.offset.get(ACRSindex);
							// AddressUnit is incremented for this cycle.
							AddressUnit++;
							// AC Reservation Station freed.
							AddCalcStation.base.remove(ACRSindex);
							AddCalcStation.DestID.remove(ACRSindex);
							AddCalcStation.offset.remove(ACRSindex);
							AddCalcStation.Value.remove(ACRSindex);
							
							LSQ.Address.set(LSQindex, memoryLocation);
							
							boolean forwarded = false;
							forwarded = checkStoreForwarding(forwarded,memoryLocation,LSQindex);							
							//Indicate for next cycle.
							if (!forwarded) {
								nextCycle.StagesAddress.add(PC);
								nextCycle.Stages.add("EXECUTE");
								LSQ.ExecState.set(LSQindex, "SECOND CYCLE");
							} else {
								// set the value at Reservation station
								LoadStation.Value.set(LSindex, LSQ.value.get(LSQindex));
								// next stage
								nextCycle.StagesAddress.add(PC);
								nextCycle.Stages.add("WRITE RESULT");
								// LSQ entry is freed.
								FreeLSQ(LSQindex);
							}
							
						} else {
							/*
							 * Wait for the Value at AC RS available.
							 */
							
							nextCycle.StagesAddress.add(PC);
							nextCycle.Stages.add("EXECUTE");
						}
						
					} else {
						// Address Unit not available.
						nextCycle.StagesAddress.add(PC);
						nextCycle.Stages.add("EXECUTE");
					}
					
				} else if (LSQ.ExecState.get(LSQindex).equalsIgnoreCase("SECOND CYCLE")) {
					long memoryLocation = LSQ.Address.get(LSQindex);
					// Check for Store-Load forwarding
					boolean forwarded = false;
					forwarded = checkStoreForwarding(forwarded,memoryLocation,LSQindex);
					if (forwarded) {
						LoadStation.Value.set(LSindex, LSQ.value.get(LSQindex));
						// next stage
						nextCycle.StagesAddress.add(PC);
						nextCycle.Stages.add("WRITE RESULT");
						FreeLSQ(LSQindex);
					} else {
						if ((LoadMemoryAccess == 1) && (checkLoadFront(memoryLocation, LSQindex))) {
							LoadMemoryAccess++;
							LSQ.ExecState.set(LSQindex, "THIRD CYCLE");
						} else {
							LSQ.ExecState.set(LSQindex, "SECOND CYCLE");
						}
						nextCycle.StagesAddress.add(PC);
						nextCycle.Stages.add("EXECUTE");
					}
				} else if (LSQ.ExecState.get(LSQindex).equalsIgnoreCase("THIRD CYCLE")) {
					long memoryLocation = LSQ.Address.get(LSQindex);
					// Check for Store-Load forwarding
					boolean forwarded = false;
					forwarded = checkStoreForwarding(forwarded,memoryLocation,LSQindex);
					if (!forwarded) {
						// Access Memory
						if (checkLoadFront(memoryLocation, LSQindex)) {
							LSQ.value.set(LSQindex, Long.parseLong(MainMemory.get(memoryLocation)));
							LoadStation.Value.set(LSindex, LSQ.value.get(LSQindex));
							// next stage
							// Set Load Memory Access Free
							LoadMemoryAccess = 1;
							// next stage
							nextCycle.StagesAddress.add(PC);
							nextCycle.Stages.add("WRITE RESULT");
							// LSQ entry is freed.
							FreeLSQ(LSQindex);
						} else {
							nextCycle.StagesAddress.add(PC);
							nextCycle.Stages.add("EXECUTE");
						}
						
						
					} else {
						// set the value at Reservation station
						LoadStation.Value.set(LSindex, LSQ.value.get(LSQindex));
						// next stage
						// Set Load Memory Access Free
						LoadMemoryAccess = 1;
						// next stage
						nextCycle.StagesAddress.add(PC);
						nextCycle.Stages.add("WRITE RESULT");
						// LSQ entry is freed.
						FreeLSQ(LSQindex);

					}
				}
			}
			if(instruction.substring(0, 2).equalsIgnoreCase("SW")) {
				int ROBindex = ROB.Id.indexOf(ROBId);
				int LSQindex = LSQ.DestID.indexOf(ROBId);
				int ACRSindex = AddCalcStation.DestID.indexOf(ROBId);
				String StoreSourceRegister = instruction.substring(3,instruction.indexOf(","));
				if (LSQ.ExecState.get(LSQindex).equalsIgnoreCase("ADDRESS CALCULATED")) {
					if (LSQ.value.get(LSQindex) == null) {
						LSQ.value.set(LSQindex, CheckROBDestination(ROBindex, StoreSourceRegister,instruction));
					}
					// Check for Store on top of LSQ and Value to write is ready or not
					boolean available = false;
					// Check for Store on top of LSQ and Value to write is ready or not
					if (LSQ.value.get(LSQindex) == null) {
						if (thisCycle.Stages.contains("WRITE RESULT")){
							
							for (int i=0;i<thisCycle.Stages.size();i++) {
								long ROBID = thisCycle.StagesAddress.get(thisCycle.Stages.indexOf("WRITE RESULT"));
								int ind = ROB.Id.indexOf((int)ROBID);
								String dest = ROB.Destination.get(ind); 
								if (dest.equalsIgnoreCase(StoreSourceRegister)) {
									available = true;
									break;
								}
							}
							
						}

					}
					
					if ((LSQindex==0) && (LSQ.value.get(LSQindex) != null) ){
						nextCycle.StagesAddress.add(PC);
						nextCycle.Stages.add("COMMIT");
						LSQ.ExecState.set(LSQindex, "FIRST COMMIT CYCLE");
					} else if (available && LSQindex ==0) {
						nextCycle.StagesAddress.add(PC);
						nextCycle.Stages.add("COMMIT");
						LSQ.ExecState.set(LSQindex, "FIRST COMMIT CYCLE");
					} else {
						nextCycle.StagesAddress.add(PC);
						nextCycle.Stages.add("EXECUTE");
						LSQ.ExecState.set(LSQindex, "ADDRESS CALCULATED");
					}

				} else if ( (AddressUnit == 1) || (AddressUnit == 2) ) {
					if(AddCalcStation.Value.get(ACRSindex)!=null){
						long memoryLocation = (long)AddCalcStation.Value.get(ACRSindex) + AddCalcStation.offset.get(ACRSindex);
						// AddressUnit is incremented for this cycle.
						AddressUnit++;
						// AC Reservation Station freed.
						FreeACStation(ACRSindex);
						// Send memoryLocation to LSQ.
						LSQ.Address.set(LSQindex, memoryLocation);
						// 
						if (LSQ.value.get(LSQindex) == null) {
							
							LSQ.value.set(LSQindex, CheckROBDestination(ROBindex, StoreSourceRegister,instruction));
						}
						boolean available = false;
						// Check for Store on top of LSQ and Value to write is ready or not
						if (LSQ.value.get(LSQindex) == null) {
							if (thisCycle.Stages.contains("WRITE RESULT")){
								
								for (int i=0;i<thisCycle.Stages.size();i++) {
									long ROBID = thisCycle.StagesAddress.get(thisCycle.Stages.indexOf("WRITE RESULT"));
									int ind = ROB.Id.indexOf((int)ROBID);
									String dest = ROB.Destination.get(ind); 
									if (dest.equalsIgnoreCase(StoreSourceRegister)) {
										available = true;
										break;
									}
								}
								
							}

						}
						
						if ((LSQindex==0) && (LSQ.value.get(LSQindex) != null) ){
							nextCycle.StagesAddress.add(PC);
							nextCycle.Stages.add("COMMIT");
							LSQ.ExecState.set(LSQindex, "FIRST COMMIT CYCLE");
						} else if (available && LSQindex ==0) {
							nextCycle.StagesAddress.add(PC);
							nextCycle.Stages.add("COMMIT");
							LSQ.ExecState.set(LSQindex, "FIRST COMMIT CYCLE");
						} else {
							nextCycle.StagesAddress.add(PC);
							nextCycle.Stages.add("EXECUTE");
							LSQ.ExecState.set(LSQindex, "ADDRESS CALCULATED");
						}
						
												

					} else {
						nextCycle.StagesAddress.add(PC);
						nextCycle.Stages.add("EXECUTE");
					}
					
				} else {
					// Address Unit not available.
					nextCycle.StagesAddress.add(PC);
					nextCycle.Stages.add("EXECUTE");
				}

								
			}
		}
		
		if(Stage.equalsIgnoreCase("WRITE RESULT")) {
			int ROBId = (int) PC;
			int index = ROB.Id.indexOf(ROBId);
			String instruction = ROB.Instruction.get(index);
			//System.out.println(" WR INST " + instruction ) ;
			String[] splitParts = instruction.split(" ");
			if (ALUInstructions.contains(splitParts[0])) {
				int ALUindex = ALUStation.DestID.indexOf(ROBId);
				int ROBindex = ROB.Id.indexOf(ROBId);
				
				for (int i=ROBindex+1;i<ROB.Id.size();i++) {
					for (int j=0;j<ALUStation.DestID.size();j++) {
						
						if (ALUStation.DestID.get(j) == ROB.Id.get(i)) {
							//System.out.println(" alu " +  ALUStation.Instruction.get(j) + "j"+ ALUStation.Vj.get(j)) ;
							//System.out.println(" alu " +  ROB.Instruction.get(ROBindex)) ;
							if( (ALUStation.Vj.get(j)!= null && ALUStation.Vj.get(j).equalsIgnoreCase(ROB.Destination.get(ROBindex))) &&
									(ALUStation.ValueVj.get(j) == null) ){
								
									ALUStation.ValueVj.set(j, ALUStation.DestValue.get(ALUindex));
								
							}
							if( (ALUStation.Vk.get(j) != null) &&
									(ALUStation.Vk.get(j).equalsIgnoreCase(ROB.Destination.get(ROBindex))) &&
									(ALUStation.ValueVk.get(j) == null) ){
								
									ALUStation.ValueVk.set(j, ALUStation.DestValue.get(ALUindex));
								
							}
						}
						
					}
					for (int j=0;j<AddCalcStation.DestID.size();j++) {
						if (AddCalcStation.DestID.get(j) == ROB.Id.get(i)) {
							if( (AddCalcStation.base.get(j).equalsIgnoreCase(ROB.Destination.get(ROBindex))) &&
									(AddCalcStation.Value.get(j) == null) ){
									AddCalcStation.Value.set(j, ALUStation.DestValue.get(ALUindex));
							}
						}
						
					}
						
				}
				// Update ROB
				ROB.Value.set(ROBindex, ALUStation.DestValue.get(ALUindex));
				// free ALU Reservation Station
				//FreeALUStation(ALUindex);
				nextCycle.StagesAddress.add(PC);
				nextCycle.Stages.add("COMMIT");
				
			}
			if(instruction.substring(0, 2).equalsIgnoreCase("LW")) {
				int LSindex = LoadStation.DestID.indexOf(ROBId);
				ROB.Value.set(index, LoadStation.Value.get(LSindex));
				// Update ALU Reservation Station
				int ROBindex = ROB.Id.indexOf(ROBId);
				
				for (int i=ROBindex+1;i<ROB.Id.size();i++) {
					for (int j=0;j<ALUStation.DestID.size();j++) {
						
						if (ALUStation.DestID.get(j) == ROB.Id.get(i)) {
							if( (ALUStation.Vj.get(j) != null && ALUStation.Vj.get(j).equalsIgnoreCase(LoadStation.Operand.get(LSindex))) &&
									(ALUStation.ValueVj.get(j) == null) ){
									ALUStation.ValueVj.set(j, LoadStation.Value.get(LSindex));
								
							}
							if( (ALUStation.Vk.get(j) != null) &&
									(ALUStation.Vk.get(j).equalsIgnoreCase(LoadStation.Operand.get(LSindex))) &&
									(ALUStation.ValueVk.get(j) == null) ){
									ALUStation.ValueVk.set(j, LoadStation.Value.get(LSindex));
								
							}
						}
						
					}
					for (int j=0;j<AddCalcStation.DestID.size();j++) {
						if (AddCalcStation.DestID.get(j) == ROB.Id.get(i)) {
							if( (AddCalcStation.base.get(j).equalsIgnoreCase(LoadStation.Operand.get(LSindex))) &&
									(AddCalcStation.Value.get(j) == null) ){
									AddCalcStation.Value.set(j, LoadStation.Value.get(LSindex));
							}
						}
						
					}
						
				}
				
				
				// free load station
				FreeLoadStation(LSindex);
				// next stage
				nextCycle.StagesAddress.add(PC);
				nextCycle.Stages.add("COMMIT");				
			}
		}
		
		if(Stage.equalsIgnoreCase("COMMIT")) {
			int ROBId = (int) PC;
			int ALUindex = ALUStation.DestID.indexOf(ROBId);
			//System.out.println("PC" +PC) ;
			//System.out.println("Robid" + ROBId) ;
			int index = ROB.Id.indexOf(ROBId);
			//System.out.println("index" + index) ;
			String instruction = ROB.Instruction.get(index);
			String[] splitParts = instruction.split(" ");
			
			if (commitInOrder(ROBId)) {
				if ((CommitPerCycle) <= 2 && (index == 0)){
					boolean doNotCountforStore = false;
					if ( splitParts[0].equalsIgnoreCase("NOP")){
						// free ROB entry
						FreeROB(index);
						//FreeALUStation(ALUindex);
						
					}
					if (splitParts[0].equalsIgnoreCase("BREAK")){
						for (int j=0;j<ROB.Id.size();j++) {
							FreeROB(j);
						}
						theEnd = true;
							
					}
					if ( ( splitParts[0].equalsIgnoreCase("J")) ||
							( Branchfirst.contains(splitParts[0])) ||
							( BranchSec.contains(splitParts[0]))
							){
						// free ROB entry
						FreeROB(index);
						FreeALUStation(ALUindex);
					}
					if ( splitParts[0].equalsIgnoreCase("LW") ){
						// update register file
						Registers.put(ROB.Destination.get(index), ROB.Value.get(index));
						// free ROB entry
						FreeROB(index);
						
					}
					if ( (ALUInstructions.contains(splitParts[0]))  ){
						// update register file
						Registers.put(ROB.Destination.get(index), ROB.Value.get(index));
						// free ROB entry
						FreeROB(index);
						FreeALUStation(ALUindex);
						
					}
					if(splitParts[0].equalsIgnoreCase("SW")) {
						int ROBindex = ROB.Id.indexOf(ROBId);
						int LSQindex = LSQ.DestID.indexOf(ROBId);
						String StoreSourceRegister = instruction.substring(3,instruction.indexOf(","));
						if (LSQ.value.get(LSQindex) == null) {
							LSQ.value.set(LSQindex, CheckROBDestination(ROBindex, StoreSourceRegister,instruction));
						}
						if ( (LSQindex == 0) && (LSQ.value.get(LSQindex) != null)){
							if (LSQ.ExecState.get(LSQindex).equalsIgnoreCase("FIRST COMMIT CYCLE")) {
								nextCycle.StagesAddress.add(PC);
								nextCycle.Stages.add("COMMIT");
								LSQ.ExecState.set(LSQindex, "SECOND COMMIT CYCLE");
							} else {
								// update Main Memory
								MainMemory.put(LSQ.Address.get(LSQindex), LSQ.value.get(LSQindex).toString());
								// free ROB entry
								FreeROB(index);
							
								// free LSQ entry
								FreeLSQ(LSQindex);
							}
						} else {
							
							nextCycle.StagesAddress.add(PC);
							nextCycle.Stages.add("COMMIT");
							doNotCountforStore = true;
						}										
					}					
					if (!doNotCountforStore) {
						CommitPerCycle++;
					}
										
				} else {
					nextCycle.StagesAddress.add(PC);
					nextCycle.Stages.add("COMMIT");
				}
					
			}
			
		}
		
	}
	
	
	private static boolean commitInOrder(int ROBId) {
		int ROBindex = ROB.Id.indexOf(ROBId);
		for (int i=0;i<thisCycle.Stages.size();i++) {
			if (thisCycle.Stages.get(i).equalsIgnoreCase("COMMIT")) {
				long compROBId = thisCycle.StagesAddress.get(i);
				int CompROBindex = ROB.Id.indexOf((int) compROBId);
				if (CompROBindex < ROBindex) {
					thisCycle.Stages.add(i+1, "COMMIT");
					thisCycle.StagesAddress.add(i+1, (long)ROBId);
					return false;
				}
			}
			
		}
		return true;
	}
	
	private static boolean checkLoadFront(long memoryLocation, int index) {
		if (index == 0) {
			return true;
		} else {
			for (int j =index-1 ; j >= 0; j--) {
				if (LSQ.Instruction.get(j).substring(0, 2).equalsIgnoreCase("SW")){
					if (LSQ.Address.get(j)!=null) {
						if (LSQ.Address.get(j)==memoryLocation) {
							return false;
						}
						
					} else {
						return false;
					}
					
				}
			}
			return true;
		}
	}
	private static boolean checkStoreForwarding(boolean forwarded, long memoryLocation, int index) {
		if (index == 0) {
			return false;
		} else {
			for (int j =index-1 ; j >= 0; j--) {
				if (LSQ.Instruction.get(j).substring(0, 2).equalsIgnoreCase("SW")){
					if (LSQ.Address.get(j)!=null) {
						if (LSQ.Address.get(j)==memoryLocation) {
							if (LSQ.value.get(j) != null) {
								LSQ.value.set(index, LSQ.value.get(j));
								return true;
							} else {
								return false;
							}
						}
					} else {
						return false;
					}
					
				}
			}
			return false;
		}
	}
	private static void MisPredictedFlush(int index){
		for (int i=index+1;i<ROB.Id.size();i++) {
			int removeROBid = ROB.Id.get(i);
			FreeROB(i);
			for (int j = 0; j<LSQ.DestID.size();j++) {
				if (LSQ.DestID.get(j) == removeROBid) {
					FreeLSQ(j);
				}
			}
			for (int j = 0; j<AddCalcStation.DestID.size();j++) {
				if (AddCalcStation.DestID.get(j) == removeROBid) {
					FreeACStation(j);
				}
			}
			for (int j = 0; j<LoadStation.DestID.size();j++) {
				if (LoadStation.DestID.get(j) == removeROBid) {
					FreeLoadStation(j);
				}
			}
			for (int j = 0; j<ALUStation.DestID.size();j++) {
				if (ALUStation.DestID.get(j) == removeROBid) {
					FreeALUStation(j);
				}
			}
			for (int j = 0; j<nextCycle.StagesAddress.size();j++) {
				if (nextCycle.StagesAddress.get(j) == removeROBid) {
					nextCycle.StagesAddress.remove(j);
					nextCycle.Stages.remove(j);
					j--;
				}
			}
			for (int j = 0; j<thisCycle.StagesAddress.size();j++) {
				if (thisCycle.StagesAddress.get(j) == removeROBid) {
					thisCycle.StagesAddress.remove(j);
					thisCycle.Stages.remove(j);
					j--;
				}
			}
		}
		for (int j = 0; j<thisCycle.StagesAddress.size();j++) {
			if (thisCycle.Stages.get(j).equalsIgnoreCase("ISSUE")){
				thisCycle.StagesAddress.remove(j);
				thisCycle.Stages.remove(j);
				j--;
			}
		}
		for (int j = 0; j<nextCycle.StagesAddress.size();j++) {
			if (nextCycle.Stages.get(j).equalsIgnoreCase("ISSUE")){
				nextCycle.StagesAddress.remove(j);
				nextCycle.Stages.remove(j);
				j--;
			}
		}
		
	}
	private static void FreeALUStation(int index){
		ALUStation.DestValue.remove(index);
		ALUStation.DestID.remove(index);
		ALUStation.Instruction.remove(index);
		ALUStation.ValueImm.remove(index);
		ALUStation.ValueVj.remove(index);
		ALUStation.ValueVk.remove(index);
		ALUStation.Vj.remove(index);
		ALUStation.Vk.remove(index);
	}
	private static void FreeLoadStation(int index){
		LoadStation.DestID.remove(index);
		LoadStation.Instruction.remove(index);
		LoadStation.Operand.remove(index);
		LoadStation.Value.remove(index);
	}
	private static void FreeLSQ(int index){
		LSQ.DestID.remove(index);
		LSQ.Address.remove(index);
		LSQ.Instruction.remove(index);
		LSQ.value.remove(index);
		LSQ.ExecState.remove(index);
	}
	private static void FreeROB(int index){
		ROB.Destination.remove(index);
		ROB.Instruction.remove(index);
		ROB.Ready.remove(index);
		ROB.Value.remove(index);
		ROB.Id.remove(index);
	}
	private static void FreeACStation(int index){
		AddCalcStation.base.remove(index);
		AddCalcStation.DestID.remove(index);
		AddCalcStation.offset.remove(index);
		AddCalcStation.Value.remove(index);
	}
	private static void BTBFree(){
		BTB.Address.remove(0);
		BTB.PredictedPC.remove(0);
		BTB.Predictor2Bit.remove(0);
		BTB.PredictorOutCome.remove(0);
		BTB.TargetPC.remove(0);
	}
}
