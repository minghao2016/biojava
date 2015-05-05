package demo;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import org.biojava.nbio.structure.Atom;
import org.biojava.nbio.structure.StructureException;
import org.biojava.nbio.structure.align.cemc.CeMcMain;
import org.biojava.nbio.structure.align.gui.StructureAlignmentDisplay;
import org.biojava.nbio.structure.align.model.MultipleAlignment;
import org.biojava.nbio.structure.align.model.StructureAlignmentException;
import org.biojava.nbio.structure.align.util.AtomCache;

/**
 * Demo for visualizing the results of a Multiple Alignment seed, before MC optimization.
 * 
 * @author Aleix Lafita
 * 
 */
public class DemoMultipleAlignmentSeed {

	public static void main(String[] args) throws IOException, StructureException, StructureAlignmentException, InterruptedException, ExecutionException {
		
		//Specify the structures to align
		List<String> names = Arrays.asList("1tim.a", "1vzw", "1nsj", "3tha.a");			//TIM barrels
		//List<String> names = Arrays.asList("1mbc", "1hlb", "1thb.a", "1ith.a");		//globins
		
		//Load the CA atoms of the structures
		AtomCache cache = new AtomCache();
		List<Atom[]> atomArrays = new ArrayList<Atom[]>();
		for (String name:names) atomArrays.add(cache.getAtoms(name));
		
		//Here the multiple structural alignment algorithm comes in place to generate the alignment object
		MultipleAlignment seed = CeMcMain.generateSeed(atomArrays);
		
		//Information about the alignment
		seed.getParent().setAlgorithmName(CeMcMain.algorithmName);
		seed.getParent().setVersion(CeMcMain.version);
        
		StructureAlignmentDisplay.display(seed);
	}
}
