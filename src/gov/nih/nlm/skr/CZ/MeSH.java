package gov.nih.nlm.skr.CZ;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.*;
import java.io.*;
import java.sql.Connection; 
import java.sql.DriverManager;
import java.sql.ResultSet; 
import java.sql.Statement;

public class MeSH {
	List MeshTreeList = null;
	Hashtable PATable = null;
	Connection conn = null;
	Statement stmt = null;
	
	public MeSH() {
		try { 
			Class.forName( "com.mysql.jdbc.Driver" ) ;
			conn = DriverManager.getConnection( "jdbc:mysql://indsrv2.nlm.nih.gov/MESH?autoReconnect=true", "root", "indsrv2@root" ) ;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	public List BuildMeSHTree(String filename) {	
		try {
			BufferedReader meshtreein
	 			= new BufferedReader(new FileReader(filename)); 
			MeshTreeList = new ArrayList();
			String line = null;
			while((line = meshtreein.readLine()) != null) {
				String compo[] = line.split(";");
				if(compo.length == 2) {
					MeshTreeList.add(compo[0]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return MeshTreeList;		
	}
	
	public List BuildPAHashtable(String filename) {	
		try {
			BufferedReader meshpain
	 			= new BufferedReader(new FileReader(filename)); 
			PATable = new Hashtable();
			String line = null;
			while((line = meshpain.readLine()) != null) {
				String compo[] = line.split("\\|");
				if(compo.length == 2) {
					PATable.put(compo[0], compo[1]);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return MeshTreeList;		
	}
	
	public int Search(String searchTerm) {
		int low = 0;
		/* int high = MeshTreeList.size()-1;
		int mid =(low + high)/2;
		boolean found = false;
		while(low <= high && low >= 0 && high < MeshTreeList.size()) {
			mid = (low + high)/2;
			if(((String)MeshTreeList.get(mid)).compareTo(searchTerm) == 0) {
				found = true;
				break;				
			} else if(((String)MeshTreeList.get(mid)).compareTo(searchTerm) > 0) 
				high = mid -1;
			else
				low = mid + 1;
		}
		
		if(found)
			return mid;
		else
			return -1;		*/
		for(int i =0; i < MeshTreeList.size(); i++) {
			if (((String)MeshTreeList.get(i)).compareTo(searchTerm) == 0) {
				return i;
			}
		}
		return -1;
				
	}
	
	public void BuildDatabaseFromMH(String filename) {
		try {
			BufferedReader meshin
	 			= new BufferedReader(new FileReader(filename)); 
			 PrintWriter meshout
		       = new PrintWriter(new BufferedWriter(new FileWriter("Q:\\LHC_Projects\\Caroline\\MESH2015_PA.txt")));
			String SQL = "INSERT into MESHINFO (TERM, PA, MN, HEADING) values (\"$1\", \"$2\", \"$3\", 1)";
			Statement stmt = conn.createStatement();
			String line = null;
			StringBuffer PA = null;
			StringBuffer MN = null;
			String key = null;
			PATable = new Hashtable();
			while((line = meshin.readLine()) != null) {
				if(line.startsWith("MH") && !line.startsWith("MH_TH")) {
					String compo[] = line.split("=");
					key = compo[1].trim();
					PA = new StringBuffer();
					MN = new StringBuffer();
				} else if(line.startsWith("PA")) {
					String compo[] = line.split("=");
					if(PA.length() == 0)
						PA.append(compo[1].trim());
					else
						PA.append(";" + compo[1].trim());
				} else if(line.startsWith("MN")) {
					String compo[] = line.split("=");
					if(MN.length() == 0)
						MN.append(compo[1].trim());
					else
						MN.append(";" + compo[1].trim());
				}  else if(line.startsWith("UI")) {
					if(PA.length() > 0) {
						PATable.put(key, true);
						meshout.println(key+ "|1");
					} else {
						PATable.put(key, false);
						// meshout.println(key+ "|0");
					}
						
					String newSQL = SQL.replace("$1", key).replace("$2", PA.toString()).replace("$3", MN.toString());
					// System.out.println(newSQL);					
					stmt.executeUpdate(newSQL);					
				}
			} // while
			meshout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void BuildDatabaseFromENTRY(String filename) {
		try {
			BufferedReader meshin
	 			= new BufferedReader(new FileReader(filename)); 
			String SQL = "INSERT into ENTRY (ENTRYTERM, MESHTERM) values (\"$1\", \"$2\")";
			Statement stmt = conn.createStatement();
			String line = null;
			String key = null;
			PATable = new Hashtable();
			while((line = meshin.readLine()) != null) {
				try {
				if(line.startsWith("MH") && !line.startsWith("MH_TH")) {
					String compo[] = line.split("=");
					key = compo[1].trim();
				} else if(line.startsWith("ENTRY") || line.startsWith("PRINT ENTRY") ) {
					String compo[] = line.split("=");
					String entries[] = compo[1].split("\\|");
					String entryterm = entries[0];						
					String newSQL = SQL.replace("$1",entryterm.trim()).replace("$2", key);
					// System.out.println(newSQL);					
					stmt.executeUpdate(newSQL);					
				}
				} catch (Exception e) {

				}
			} // while
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public void BuildDatabaseFromSP(String filename) {
		try {
			BufferedReader meshin
	 			= new BufferedReader(new FileReader(filename)); 
			 /* PrintWriter meshout
		       = new PrintWriter(new BufferedWriter(new FileWriter("Q:\\LHC_Projects\\Caroline\\MESH2015_PA.txt", true))); */
			String SQL = "INSERT into MESHINFO (TERM, PA, II, HEADING) values (\"$1\", \"$2\", \"$3\", 0)";
			Statement stmt = conn.createStatement();
			String line = null;
			StringBuffer PA = null;
			StringBuffer II = null;
			String key = null;
			List SYList = new ArrayList();
			int num = 0;
			while((line = meshin.readLine()) != null) {
				if(line.startsWith("NM") && !line.startsWith("NM_TH")) {
					String compo[] = line.split("=");
					key = compo[1].trim();
					PA = new StringBuffer();
					II = new StringBuffer();
				} else if(line.startsWith("PA")) {
					String compo[] = line.split("=");
					if(PA.length() == 0)
						PA.append(compo[1].trim());
					else
						PA.append(";" + compo[1].trim());
				} else if(line.startsWith("II")) {
					String compo[] = line.split("=");
					if(II.length() == 0)
						II.append(compo[1].trim());
					else
						II.append(";" + compo[1].trim());
				}  else if(line.startsWith("SY")) {
					String compo1[] = line.split("=");
					if(compo1.length > 1) {
						String compo2[] = compo1[1].split("\\|");
						if(compo2.length > 0)
							SYList.add(compo2[0].trim());
					}

				} else if(line.startsWith("UI")) {
					if(PA.length() > 0) {
						// PATable.put(key, true);
						// meshout.println(key+ "|1");
					} else {
						// PATable.put(key, false);
						// meshout.println(key+ "|0");
					}
					String IIStr = null;

					// System.out.println("key = " + key);
					if(key.length() > 200)
						key = key.substring(0,200);
					if(II.length() > 200)
						IIStr = II.substring(0,200);
					else
						IIStr = II.toString();
					// String newSQL = SQL.replace("$1", key).replace("$2", PA.toString()).replace("$3", IIStr);
					for(int i = 0; i < SYList.size(); i++) {
						String synonym = (String) SYList.get(i);
						String SY_SQL = SQL.replace("$1", synonym).replace("$2", PA.toString()).replace("$3", IIStr);
						num++;
						// System.out.println(num);
						try {
							stmt.executeUpdate(SY_SQL);		
						} catch (Exception e) {
							// System.out.println("SQL error|" + SY_SQL);
						}
					}
					SYList = new ArrayList();
				}
			} // while
			// meshout.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
	
	public MESHINFO SearchMESHDatabase(String searchTerm) {
		MESHINFO meshinfo = new MESHINFO();
		try {
			String SQLquery = "select PA, MN, II, HEADING, PRECONCEPT from MESHINFO where TERM = \"";
			String SQLquery2 = "select PA, MN, II, HEADING, PRECONCEPT from MESHINFO where TERM = \"" + searchTerm + "s\"";
			String SQLqueryEntry = "select ENTRYTERM, MESHTERM from ENTRY where ENTRYTERM = \"" + searchTerm + "\"";
			Statement stmt = conn.createStatement();
			
			/**
			 * Check if search term is an entry term for mesh term
			 * ERROR found on April 18 2015 that Deprenyl is not in the MESHINFO table since it is in MESH2015 table as ENTRY or PRINT ENTRY TERM
			 */
			boolean entryFound = false;
			// System.out.println(SQLqueryEntry);
			ResultSet rsEntry = stmt.executeQuery(SQLqueryEntry);
			if(rsEntry.next()) {
				// meshinfo.TERM = rs.getString(1);
				entryFound = true;
				String term = rsEntry.getString("MESHTERM");
				// System.out.println("MESHTERMFOUND = " + term + " from " + searchTerm);
				SQLquery = SQLquery + term + "\"";
			} else {
				SQLquery = SQLquery + searchTerm + "\"";
			}
			// System.out.println(SQLquery);
			ResultSet rs = stmt.executeQuery(SQLquery);

			if(rs.next()) {
				// meshinfo.TERM = rs.getString(1);
				meshinfo.TERM = searchTerm;
				meshinfo.PA = rs.getString(1);
				meshinfo.MN = rs.getString(2);
				meshinfo.II = rs.getString(3);
				meshinfo.HEADING = rs.getInt(4);
				meshinfo.PRECONCEPT = rs.getString(5);
				rs.close();
			}
			/**
			 * Error fix for a citation 26317803.ab.9
			 * SE|26317803||ab|9|entity|C1512474|Histone deacetylase inhibitor|aapp,enzy,phsu|||histone deacetylase inhibitor||||893|1736|1765
			 * In this case, if the original search term is not found, try to find any term that is variant of the term. For instance,
			 * the term "C1512474|Histone deacetylase inhibitor" can only be found as "Histone deacetylase inhibitors" in MESHINFO.
			 * So the DB search needs to be "select * from MESHINFO where TERM = ""Histone deacetylase inhibitors"
			 */
			else if(!searchTerm.endsWith("s") && !entryFound) { 
				rs.close();
				ResultSet rs2 = stmt.executeQuery(SQLquery2);
				// System.out.println("Try more general query : " + SQLquery2);
				if(rs2.next() ) {
					meshinfo.TERM = searchTerm;
					meshinfo.PA = rs2.getString(1);
					meshinfo.MN = rs2.getString(2);
					meshinfo.II = rs2.getString(3);
					meshinfo.HEADING = rs2.getInt(4);
					meshinfo.PRECONCEPT = rs2.getString(5);
				}
				rs2.close();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return meshinfo;
	}
	
	public void BuildMESHPreconcept(String filename) {
		try {
			BufferedReader meshin
	 			= new BufferedReader(new FileReader(filename)); 
			// PrintWriter meshout
		    //   = new PrintWriter(new BufferedWriter(new FileWriter("Q:\\LHC_Projects\\Caroline\\mtree2015.txt", true)));
			Hashtable HierarchyTable = new Hashtable();
			String SQL = "UPDATE MESHINFO set PRECONCEPT = \"$1\" WHERE TERM = \"$2\"";
			Statement stmt = conn.createStatement();
			String line = null;
			String concept = null;
			String preconcept = null;
			String hierarchy = null;
			int num = 0;
			while((line = meshin.readLine()) != null) {
				String compo[] = line.split(";");
				preconcept = concept;
				concept = compo[0];
				hierarchy = compo[1];
				HierarchyTable.put(compo[1], compo[0]);
				int lastDotPos = compo[1].lastIndexOf('.');
				String parentHierarchy = null;
				if(lastDotPos > 0)
					parentHierarchy = compo[1].substring(0, lastDotPos);
				String parentConcept = null;
				if(parentHierarchy != null)
					parentConcept = (String) HierarchyTable.get(parentHierarchy);
				if(num != 0 && concept != null && parentConcept != null) {
					String thisSQL = SQL.replace("$1", parentConcept).replace("$2", concept);
					// System.out.println(thisSQL);
					try {
						stmt.executeUpdate(thisSQL);		
					} catch (Exception e) {
						// System.out.println("SQL error|" + SY_SQL);
					} 
				}
				num++;
			}
			// System.out.println("num of update = " + num);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]) {
		try {
			String in1 = "Q:\\LHC_Projects\\Caroline\\data\\MESH\\MeSH2015"; 
			String in2 = "Q:\\LHC_Projects\\Caroline\\data\\MESH\\supp2015.txt"; 
			MeSH mesh = new MeSH();
			// mesh.BuildDatabaseFromMH(in1);
			// mesh.BuildDatabaseFromSP(in2);
			// mesh.BuildMESHPreconcept("Q:\\LHC_Projects\\Caroline\\data\\MESH\\mtree2015.txt");
			 mesh.BuildDatabaseFromENTRY(in1);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
