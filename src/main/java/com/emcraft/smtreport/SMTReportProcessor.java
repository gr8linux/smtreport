package com.emcraft.smtreport;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.supercsv.cellprocessor.Optional;
import org.supercsv.cellprocessor.ParseInt;
import org.supercsv.cellprocessor.constraint.NotNull;
import org.supercsv.cellprocessor.ift.CellProcessor;
import org.supercsv.io.CsvListReader;
import org.supercsv.io.ICsvListReader;
import org.supercsv.prefs.CsvPreference;

public class SMTReportProcessor {

	public static int BOARD_NAME = 1;
	public static int PCB_ASSEMBLED = 4;
	public static int COMPONENT = 8;
	public static int NUMBER_PLACED = 9;
	public static int MECHNICAL_FAILURE = 11;
	public static int ELECTRICAL_FAILURE = 12;
	public static int PICKING_FAILURE = 13;
	public static int PLACEMENT_FAILURE = 14;
	public static int OTHER_FAILURE = 15;
	public static int CONSUMED = 16;
	public static String BOARD_REPORT = "-b";
	public static String COMPONENTS_REPORT = "-u";

	public static void main(String[] args) {
		// Uncomment this for testing
		// if (args == null || args.length == 0) {
		// args = new String[] { COMPONENTS_REPORT, "file_1.txt", "file_2.txt"
		// };
		// }
		if (args != null && args.length == 3) {
			File f1 = new File(args[1]);
			File f2 = new File(args[2]);
			if (f1.exists() && f2.exists()) {
				try {
					new SMTReportProcessor().generateReport(args[0], args[1],
							args[2]);
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("Invalid FilePath");
			}
		} else {
			System.out.println("Parameter Missing");
		}
	}

	public void generateReport(String type, String file1, String file2)
			throws Exception {
		Map<String, List<Object>> file1Map = readFile(file1,
				CsvPreference.TAB_PREFERENCE);
		Map<String, List<Object>> file2Map = readFile(file2,
				CsvPreference.TAB_PREFERENCE);
		if (BOARD_REPORT.equals(type)) {
			generateBoradsReport(file1Map, file2Map);
		} else if (COMPONENTS_REPORT.equals(type)) {
			generateComponentsReport(file1Map, file2Map);
		} else {
			System.out.println("\n" + " Invalid Command ");
		}
	}

	protected Map<String, List<Object>> readFile(String fileSource,
			CsvPreference csvPreference) throws Exception {
		ICsvListReader listReader = null;
		List<Object> reportList = null;
		Map<String, List<Object>> map = new HashMap<String, List<Object>>();
		try {
			listReader = new CsvListReader(new FileReader(fileSource),
					csvPreference);
			listReader.getHeader(true);

			String nextBoardName = "";
			String prevkey = "";
			List<Object> rows = new ArrayList<Object>();
			while (listReader.read() != null) {

				int numColumns = listReader.length();

				reportList = listReader
						.executeProcessors(getProcessors(numColumns));

				String layoutName = (String) reportList.get(BOARD_NAME);
				nextBoardName = (layoutName == null ? "" : layoutName);
				if (!("").equals(nextBoardName) && !("").equals(prevkey)) {
					map.put(prevkey, rows);
					rows = new ArrayList<Object>();
					rows.add(reportList);
				} else {
					rows.add(reportList);
				}
				if (layoutName != null)
					prevkey = layoutName;
			}
			map.put(prevkey, rows); // last Group Entry
		} finally {
			if (listReader != null) {
				listReader.close();
			}
		}
		return map;
	}

	private CellProcessor[] getProcessors(int howMany) {
		List<CellProcessor> processors = new ArrayList<CellProcessor>();

		processors.add(null); // empty
		processors.add(null); // Layout name
		processors.add(null); // Station
		processors.add(null); // Layouts assembled
		processors.add(null); // PCBs assembled
		processors.add(null); // Layout load time[s]
		processors.add(null); // Assembly time[s]/PCB
		processors.add(null); // Glue time[s]/PCB
		processors.add(new NotNull()); // Component
		processors.add(new NotNull()); // Number placed
		processors.add(null); // Time[s]/comp.
		processors.add(new Optional(new ParseInt())); // Mechanical failures
		processors.add(new Optional(new ParseInt())); // Electrical failures
		processors.add(new Optional(new ParseInt())); // Picking failures
		processors.add(new Optional(new ParseInt())); // Placement failures
		processors.add(new Optional(new ParseInt())); // Other failures
		processors.add(new Optional(new ParseInt())); // Consumed

		return processors.subList(0, howMany).toArray(new CellProcessor[0]);
	}

	private void generateBoradsReport(Map<String, List<Object>> file1,
			Map<String, List<Object>> file2) {

		for (String key : file2.keySet()) {

			List<Object> filelist1 = file1.get(key);
			List<Object> filelist2 = file2.get(key);

			if (filelist1 == null && filelist2 != null) {
				List<?> row1 = (List<?>) filelist2.get(0);
				String qty1 = (String) row1.get(PCB_ASSEMBLED);// PCBs assembled
				int totalQty = Integer.valueOf(qty1);
				System.out.println(row1.get(BOARD_NAME) + "\t\t"
						+ Math.abs(totalQty));

			} else {
				List<?> row1 = (List<?>) filelist1.get(0);
				List<?> row2 = (List<?>) filelist2.get(0);
				String qty1Column = (String) row1.get(PCB_ASSEMBLED);
				String qty2Column = (String) row2.get(PCB_ASSEMBLED);
				int qty1 = qty1Column == null ? 0 : Integer.valueOf(qty1Column);
				int qty2 = qty2Column == null ? 0 : Integer.valueOf(qty2Column);

				int totalQty = qty1 - qty2;
				System.out.println(row1.get(BOARD_NAME) + "\t\t"
						+ Math.abs(totalQty));
			}

		}
		// If a component is mentioned in file1,but not in file2,
		generateErrorReport(file1, file2);

	}

	private void generateErrorReport(Map<String, List<Object>> file1,
			Map<String, List<Object>> file2) {
		if (file1 != null) {
			for (String key : file1.keySet()) {
				List<Object> filelist1 = file1.get(key);
				List<Object> filelist2 = file2.get(key);
				if (filelist1 != null && filelist2 == null) {
					List<?> row1 = (List<?>) filelist1.get(0);
					System.out.println("\n\n\n\n");
					System.out
							.println(String
									.format("--------------Component is Mentioned in File1, but not in File2------------------"));
					System.out.println("BoardName" + "\t\t\t\t");
					System.out
							.println("-----------------------------------------------");
					System.out.println(row1.get(BOARD_NAME) + "\t\t");
				}
			}
		}
	}

	private void generateComponentsReport(Map<String, List<Object>> file1,
			Map<String, List<Object>> file2) {

		for (String key : file2.keySet()) {
			List<Object> filelist1 = file1.get(key);
			List<Object> filelist2 = file2.get(key);
			System.out.println(String.format("%20s,%15s,%15s,%15s",
					"Component Name", "Qty Placed", "Qty Attrition",
					"Qty Total"));
			System.out
					.println(String
							.format("-----------------------------------------------------------------------"));

			if (filelist1 == null && filelist2 != null) {
				for (int i = 0; i < filelist2.size(); i++) {
					List<?> row = (List<?>) filelist2.get(i);
					String qty = (String) row.get(NUMBER_PLACED);
					int qtyPlaced = Integer.valueOf(qty);
					int qtyTotal = (Integer) row.get(CONSUMED);
					int qtyAttrition = (Integer) row.get(MECHNICAL_FAILURE)
							+ (Integer) row.get(ELECTRICAL_FAILURE)
							+ (Integer) row.get(PICKING_FAILURE)
							+ (Integer) row.get(PLACEMENT_FAILURE)
							+ (Integer) row.get(OTHER_FAILURE);
					System.out.println(String.format("%20s,%15s,%15s,%15s",
							row.get(COMPONENT), Math.abs(qtyPlaced),
							Math.abs(qtyAttrition), Math.abs(qtyTotal)));
				}
			} else {
				for (int i = 0; i < filelist2.size(); i++) {
					List<?> row1 = (List<?>) filelist1.get(i);
					List<?> row2 = (List<?>) filelist2.get(i);
					if (row1.size() <= NUMBER_PLACED
							|| row2.size() <= NUMBER_PLACED) {
						/*
						 * This means we have no assembly data in this file.
						 */
						continue;
					}
					String qty1 = (String) row1.get(NUMBER_PLACED);
					String qty2 = (String) row2.get(NUMBER_PLACED);
					int qtyPlaced = Integer.valueOf(qty1)
							- Integer.valueOf(qty2);
					int qtyTotal1 = (Integer) row1.get(CONSUMED);
					int qtyTotal2 = (Integer) row2.get(CONSUMED);
					int qtyTotal = qtyTotal1 - qtyTotal2;
					int failuefile1 = (Integer) row1.get(MECHNICAL_FAILURE)
							+ (Integer) row1.get(ELECTRICAL_FAILURE)
							+ (Integer) row1.get(PICKING_FAILURE)
							+ (Integer) row1.get(PLACEMENT_FAILURE)
							+ (Integer) row1.get(OTHER_FAILURE);
					int failuefile2 = (Integer) row2.get(MECHNICAL_FAILURE)
							+ (Integer) row2.get(ELECTRICAL_FAILURE)
							+ (Integer) row2.get(PICKING_FAILURE)
							+ (Integer) row2.get(PLACEMENT_FAILURE)
							+ (Integer) row2.get(OTHER_FAILURE);
					int qtyAttrition = failuefile1 - failuefile2;
					System.out.println(String.format("%20s,%15s,%15s,%15s",
							row1.get(COMPONENT), Math.abs(qtyPlaced),
							Math.abs(qtyAttrition), Math.abs(qtyTotal)));
				}
			}
		}
		// If a component is mentioned in file1,but not in file2,
		generateErrorReport(file1, file2);
	}
}
