package FPGrowthJava;

import java.io.BufferedReader; 
import java.io.File; 
import java.io.FileReader; 
import java.io.FileWriter;
import java.io.IOException; 
import java.util.ArrayList; 
import java.util.Collections; 
import java.util.Comparator; 
import java.util.HashMap; 
import java.util.Iterator; 
import java.util.LinkedList; 
import java.util.List; 
import java.util.Map; 
import java.util.Map.Entry; 
import java.util.Set; 


public class FpGrowth { 
	  
    private int minSup; // Minimum Destek 
    public int getMinSup() { 
        return minSup; 
    } 
  
    public void setMinSup(int minSup) { 
        this.minSup = minSup; 
    } 
    
//1. İşlem Kayıtlarını Okuma
    public List<List<String>> readTransData(String filename) { 
        List<List<String>> records = new LinkedList<List<String>>(); 
        List<String> record; 
        try { 
            FileReader fr = new FileReader(new File(filename)); 
            BufferedReader br = new BufferedReader(fr); 
            String line = null; 
            while ((line = br.readLine()) != null) { 
                if (line.trim() != "") { 
                    record = new LinkedList<String>(); 
                    String[] items = line.split(" "); 
                    for (String item : items) { 
                        record.add(item); 
                    } 
                    records.add(record); 
                } 
            } 
        } catch (IOException e) { 
            System.out.println("İşlem veritabanı okunamadı."); 
            System.exit(-2); 
        } 
        return records; 
    } 
  
// 2. Sık Öğe Kümesi Oluşturma
    public ArrayList<TreeNode> buildF1Items(List<List<String>> transRecords) { 
        ArrayList<TreeNode> F1 = null; 
        if (transRecords.size() > 0) { 
            F1 = new ArrayList<TreeNode>(); 
            Map<String, TreeNode> map = new HashMap<String, TreeNode>(); 
            // İşlem veritabanındaki her bir öğenin desteğini hesaplayın
            for (List<String> record : transRecords) { 
                for (String item : record) { 
                    if (!map.keySet().contains(item)) { 
                        TreeNode node = new TreeNode(item); 
                        node.setCount(1); 
                        map.put(item, node); 
                    } else { 
                        map.get(item).countIncrement(1); 
                    } 
                } 
            } 
            // MinSup'tan F1'e kadar (veya ona eşit) desteği olan öğeler eklenir
            Set<String> names = map.keySet(); 
            for (String name : names) { 
                TreeNode tnode = map.get(name); 
                if (tnode.getCount() >= minSup) { 
                    F1.add(tnode); 
                } 
            } 
            Collections.sort(F1); 
            return F1; 
        } else { 
            return null; 
        } 
    } 

//3. FP Agacı Olusturma
    public TreeNode buildFPTree(List<List<String>> transRecords, 
            ArrayList<TreeNode> F1) { 
        TreeNode root = new TreeNode(); //Agacın kök düğümünü oluşturma
        for (List<String> transRecord : transRecords) { 
            LinkedList<String> record = sortByF1(transRecord, F1); 
            TreeNode subTreeRoot = root; 
            TreeNode tmpRoot = null; 
            if (root.getChildren() != null) { 
                while (!record.isEmpty() 
                        && (tmpRoot = subTreeRoot.findChild(record.peek())) != null) { // İlk öğeyi alınır
                    tmpRoot.countIncrement(1); 
                    subTreeRoot = tmpRoot; // Seviye geçişi 
                    record.poll(); //
                } 
            } 
            addNodes(subTreeRoot, record, F1); 
        } 
        return root; 
    } 
  

// 3.1 F1'deki sıraya göre işlem veritabanındaki bir kaydı sıralama
    public LinkedList<String> sortByF1(List<String> transRecord, 
            ArrayList<TreeNode> F1) { 
        Map<String, Integer> map = new HashMap<String, Integer>(); 
        for (String item : transRecord) { 
        	// F1 zaten azalan sırada olduğundan
            for (int i = 0; i < F1.size(); i++) { 
                TreeNode tnode = F1.get(i); 
                if (tnode.getName().equals(item)) { 
                    map.put(item, i); 
                } 
            } 
        } 
        ArrayList<Entry<String, Integer>> al = new ArrayList<Entry<String, Integer>>( 
                map.entrySet()); 
        Collections.sort(al, new Comparator<Map.Entry<String, Integer>>() { 
            @Override
            public int compare(Entry<String, Integer> arg0, 
                    Entry<String, Integer> arg1) { 
            		// Azalan sıralama
                return arg0.getValue() - arg1.getValue(); 
            } 
        }); 
        LinkedList<String> rest = new LinkedList<String>(); 
        for (Entry<String, Integer> entry : al) { 
            rest.add(entry.getKey()); 
        } 
        return rest; 
    } 
  
// 3.2 Ağaca, belirtilen düğümün soyundan gelen birkaç düğüm eklenir.
    public void addNodes(TreeNode ancestor, LinkedList<String> record, 
            ArrayList<TreeNode> F1) { 
        if (record.size() > 0) { 
            while (record.size() > 0) { 
                String item = record.poll(); 
                TreeNode leafnode = new TreeNode(item); 
                leafnode.setCount(1); 
                leafnode.setParent(ancestor); 
                ancestor.addChild(leafnode); 
  
                for (TreeNode f1 : F1) { 
                    if (f1.getName().equals(item)) { 
                        while (f1.getNextHomonym() != null) { 
                            f1 = f1.getNextHomonym(); 
                        } 
                        f1.setNextHomonym(leafnode); 
                        break; 
                    } 
                } 
  
                addNodes(leafnode, record, F1); 
            } 
        } 
    } 
  
// 4. FPTree'deki tüm sık öğe kalıpları bulunur.
    public Map<List<String>, Integer> findFP(TreeNode root, 
            ArrayList<TreeNode> F1) { 
        Map<List<String>, Integer> fp = new HashMap<List<String>, Integer>(); 
  
        Iterator<TreeNode> iter = F1.iterator(); 
        while (iter.hasNext()) { 
            TreeNode curr = iter.next(); 
            // cur'un koşullu mod temel CPB'sini bulun ve transRecords'a koyun
            List<List<String>> transRecords = new LinkedList<List<String>>(); 
            TreeNode backnode = curr.getNextHomonym(); 
            while (backnode != null) { 
                int counter = backnode.getCount(); 
                List<String> prenodes = new ArrayList<String>(); 
                TreeNode parent = backnode; 
                // Backnode'un üst düğümlerine geçilir ve ön düğümlere yerleştirilir
                while ((parent = parent.getParent()).getName() != null) { 
                    prenodes.add(parent.getName()); 
                } 
                while (counter-- > 0) { 
                    transRecords.add(prenodes); 
                } 
                backnode = backnode.getNextHomonym(); 
            } 
  
         // Sık 1 öğe kümesi oluşturulur
            ArrayList<TreeNode> subF1 = buildF1Items(transRecords); 
         // Koşullu model tabanının yerel bir FP ağacını oluşturulur
            TreeNode subRoot = buildFPTree(transRecords, subF1); 
  
         // Koşullu FP-Ağacından sık kalıplar bulunur
            if (subRoot != null) { 
                Map<List<String>, Integer> prePatterns = findPrePattern(subRoot); 
                if (prePatterns != null) { 
                    Set<Entry<List<String>, Integer>> ss = prePatterns 
                            .entrySet(); 
                    for (Entry<List<String>, Integer> entry : ss) { 
                        entry.getKey().add(curr.getName()); 
                        fp.put(entry.getKey(), entry.getValue()); 
                    } 
                } 
            } 
        } 
  
        return fp; 
    } 
  

//4.1 Bir FP-Ağacından tüm önek modellerini bulun

    public Map<List<String>, Integer> findPrePattern(TreeNode root) { 
        Map<List<String>, Integer> patterns = null; 
        List<TreeNode> children = root.getChildren(); 
        if (children != null) { 
            patterns = new HashMap<List<String>, Integer>(); 
            for (TreeNode child : children) { 
            	// Alt ağaçtaki tüm uzun yolları kök düğüm olarak alt öğe olarak bulunur (uzun yol, başka bir yolun alt yolu olmadığı anlamına gelir) 
                LinkedList<LinkedList<TreeNode>> paths = buildPaths(child); 
                if (paths != null) { 
                    for (List<TreeNode> path : paths) { 
                        Map<List<String>, Integer> backPatterns = combination(path); 
                        Set<Entry<List<String>, Integer>> entryset = backPatterns 
                                .entrySet(); 
                        for (Entry<List<String>, Integer> entry : entryset) { 
                            List<String> key = entry.getKey(); 
                            int c1 = entry.getValue(); 
                            int c0 = 0; 
                            if (patterns.containsKey(key)) { 
                                c0 = patterns.get(key).byteValue(); 
                            } 
                            patterns.put(key, c0 + c1); 
                        } 
                    } 
                } 
            } 
        } 
  
     // MinSup'tan daha az olan modları filtrelenir.
        Map<List<String>, Integer> rect = null; 
        if (patterns != null) { 
            rect = new HashMap<List<String>, Integer>(); 
            Set<Entry<List<String>, Integer>> ss = patterns.entrySet(); 
            for (Entry<List<String>, Integer> entry : ss) { 
                if (entry.getValue() >= minSup) { 
                    rect.put(entry.getKey(), entry.getValue()); 
                } 
            } 
        } 
        return rect; 
    } 
  

// 4.1.1 Belirtilen düğümden (kök) tüm erişilebilir yaprak düğümlere giden yolu bulunur.
    public LinkedList<LinkedList<TreeNode>> buildPaths(TreeNode root) { 
        LinkedList<LinkedList<TreeNode>> paths = null; 
        if (root != null) { 
            paths = new LinkedList<LinkedList<TreeNode>>(); 
            List<TreeNode> children = root.getChildren(); 
            if (children != null) { 
            	// Tek bir yolu ağaçtan ayırırken, çatal düğümü için, sayısı da her yola bölünmelidir
                // FP-Tree'nin koşulu birden fazla daldır
                if (children.size() > 1) { 
                    for (TreeNode child : children) { 
                        int count = child.getCount(); 
                        LinkedList<LinkedList<TreeNode>> ll = buildPaths(child); 
                        for (LinkedList<TreeNode> lp : ll) { 
                                TreeNode prenode = new TreeNode(root.getName()); 
                                prenode.setCount(count); 
                                lp.addFirst(prenode); 
                            paths.add(lp); 
                        } 
                    } 
                } 

             // FP-Tree'nin tek dal olması şartı
                else{ 
                    for (TreeNode child : children) { 
                        LinkedList<LinkedList<TreeNode>> ll = buildPaths(child); 
                        for (LinkedList<TreeNode> lp : ll) { 
                            lp.addFirst(root); 
                            paths.add(lp); 
                        } 
                    } 
                } 
            } else { 
                LinkedList<TreeNode> lp = new LinkedList<TreeNode>(); 
                lp.add(root); 
                paths.add(lp); 
            } 
        } 
        return paths; 
    } 
  
    /** 
     * 4.1.2
     * dosyadaki tüm öğelerin herhangi bir kombinasyonunu oluşturulur ve her kombinasyonun sayısı yazılır.
     * aslında, kombinasyondaki son öğenin sayısıdır, çünkü kombinasyon algoritmamız ağacı garanti eder
     * (Veya yolda) ve kombinasyondaki öğelerin göreli sırası değişmeden kalır
     */
    public Map<List<String>, Integer> combination(List<TreeNode> path) { 
        if (path.size() > 0) { 
        	// İlk düğümü kaldırılır
            TreeNode start = path.remove(0); 
            // İlk düğümün kendisi bir kombinasyon haline gelebilir.
            Map<List<String>, Integer> rect = new HashMap<List<String>, Integer>(); 
            List<String> li = new ArrayList<String>(); 
            li.add(start.getName()); 
            rect.put(li, start.getCount()); 
  
            Map<List<String>, Integer> postCombination = combination(path); 
            if (postCombination != null) { 
                Set<Entry<List<String>, Integer>> set = postCombination 
                        .entrySet(); 
                for (Entry<List<String>, Integer> entry : set) { 

                	// İlk düğümden sonraki tüm öğe kombinasyonlarını rect
                    rect.put(entry.getKey(), entry.getValue()); 
                    // İlk düğümün ve sonraki öğelerin çeşitli kombinasyonları yerleştirilir.
                    List<String> ll = new ArrayList<String>(); 
                    ll.addAll(entry.getKey()); 
                    ll.add(start.getName()); 
                    rect.put(ll, entry.getValue()); 
                } 
            } 
  
            return rect; 
        } else { 
            return null; 
        } 
    } 
  

// Sık 1 öğe seti çıktı
    public void printF1(List<TreeNode> F1) { 
        System.out.println("F-1 set: "); 
        for (TreeNode item : F1) { 
            System.out.print(item.getName() + ":" + item.getCount() + "\t"); 
        } 
        System.out.println(); 
        System.out.println(); 
    } 
  

// FP Ağacı Yazdırma
    public void printFPTree(TreeNode root) { 
        printNode(root); 
        List<TreeNode> children = root.getChildren(); 
        if (children != null && children.size() > 0) { 
            for (TreeNode child : children) { 
                printFPTree(child); 
            } 
        } 
    } 
  

// Ağaçtaki tek bir düğümün bilgilerini yazdırın
    public void printNode(TreeNode node) { 
        if (node.getName() != null) { 
            System.out.print("Name:" + node.getName() + "\tCount:"
                    + node.getCount() + "\tParent:"
                    + node.getParent().getName()); 
            if (node.getNextHomonym() != null) 
                System.out.print("\tNextHomonym:"
                        + node.getNextHomonym().getName()); 
            System.out.print("\tChildren:"); 
            node.printChildrenName(); 
            System.out.println(); 
        } else { 
            System.out.println("FPTreeRoot"); 
        } 
    } 
  

// Son olarak bulunan tüm sık öğe setleri yazdırılır 
    public void printFreqPatterns(Map<List<String>, Integer> patterns, String transFile, ArrayList<TreeNode> f1) throws IOException { 
        System.out.println(); 
        System.out.println("MinSupport=" + this.getMinSup()); 
        System.out.println("Total number of Frequent Patterns is :" + patterns.size());
        System.out.println("Frequent Patterns and their Support are written to file");
        String shortFileName = transFile.split("/")[1];
        FileWriter FPResFile = new FileWriter(new File("D:/" + shortFileName.substring(0, shortFileName.indexOf("."))+"_fp_minSup"+this.getMinSup()+"_size"+patterns.size()));
        FPResFile.append("MinSupport=" + this.getMinSup()+"\n");
        int total = patterns.size()+ f1.size();
        FPResFile.append("Total number of Frequent Patterns is :" + total +"\n");
        FPResFile.append("Frequent Patterns and their Support\n");

        // İlk çıktı sık öğe setleri ve destekleri
        for(TreeNode tn : f1){
        	FPResFile.append(tn.getName() + ":" + tn.getCount() +"\n"); 
        }
        Set<Entry<List<String>, Integer>> ss = patterns.entrySet(); 
        for (Entry<List<String>, Integer> entry : ss) { 
            List<String> list = entry.getKey(); 
            for (String item : list) { 
            	FPResFile.append(item + " "); 
            } 
            FPResFile.append("："+entry.getValue()+"\n"); 
            FPResFile.flush();
           
        } 
    } 
  
    public static void main(String[] args) throws IOException { 
        FpGrowth fptree = new FpGrowth(); 
        String transFile = "D:/kayitlistesi.txt";
        List<List<String>> transRecords = fptree.readTransData(transFile); // İlk test grubu
        
        fptree.setMinSup((int)(transRecords.size() * 0.25));
        long startTime = System.currentTimeMillis();
        ArrayList<TreeNode> F1 = fptree.buildF1Items(transRecords); 
        fptree.printF1(F1); 
        TreeNode treeroot = fptree.buildFPTree(transRecords, F1); 
        fptree.printFPTree(treeroot); 
        Map<List<String>, Integer> patterns = fptree.findFP(treeroot, F1); 
        System.out.println("size of F1 = "+F1.size());
        long endTime = System.currentTimeMillis();
		System.out.println("Paylaşırken:" + (endTime - startTime) + "ms");
        fptree.printFreqPatterns(patterns, transFile, F1); 
    } 
}
