package com.example.changeloggenerator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Row.MissingCellPolicy;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;


//@SpringBootApplication
public class ChangelogGeneratorApplication {

	public static void main(String[] args) throws EncryptedDocumentException, InvalidFormatException, IOException {
//		SpringApplication.run(ChangelogGeneratorApplication.class, args);
		excelReaderDemo2();
	}
	
	public static void excelReaderDemo2() throws IOException, EncryptedDocumentException, InvalidFormatException {
		Scanner scanner = new Scanner(System.in);
		System.out.println("input file path: ");
		String filePath = scanner.nextLine();
		
		// Creating a Workbook from an Excel file (.xls or .xlsx)
        Workbook workbook = WorkbookFactory.create(new File(filePath));

        for(int i = 0 ; i < workbook.getNumberOfSheets() ; i++ ) {
            Sheet sheet = workbook.getSheetAt(i);
            handleSheet(sheet);	
        }
        
        // Closing the workbook
        workbook.close();
	}
	
	public static String handleSheet(Sheet sheet) throws JsonProcessingException {
        List<Column> columns = new ArrayList<Column>();
        for (int i=0 ; i< sheet.getPhysicalNumberOfRows(); i++) {
        	if(i == 0)
        		continue;
        	Row row = sheet.getRow(i);
            Column column = new Column();        	
    		column.setColumn(Optional.ofNullable(row.getCell(0, MissingCellPolicy.RETURN_NULL_AND_BLANK)).map(item -> item.toString()).orElse(null));
    		column.setType(Optional.ofNullable(row.getCell(1, MissingCellPolicy.RETURN_NULL_AND_BLANK)).map(item -> item.toString()).orElse(null));
    		column.setNotNull(Optional.ofNullable(row.getCell(2, MissingCellPolicy.RETURN_NULL_AND_BLANK)).map(item -> item.toString()).orElse(null));
    		column.setPk(Optional.ofNullable(row.getCell(3, MissingCellPolicy.RETURN_NULL_AND_BLANK)).map(item -> item.toString()).orElse(null));
    		column.setUk(Optional.ofNullable(row.getCell(4, MissingCellPolicy.RETURN_NULL_AND_BLANK)).map(item -> item.toString()).orElse(null));
    		column.setFk(Optional.ofNullable(row.getCell(5, MissingCellPolicy.RETURN_NULL_AND_BLANK)).map(item -> item.toString()).orElse(null));
    		column.setIndex(Optional.ofNullable(row.getCell(6, MissingCellPolicy.RETURN_NULL_AND_BLANK)).map(item -> item.toString()).orElse(null));
        	columns.add(column);
        }
		
		ObjectMapper mapper = new ObjectMapper();
		
        String tableName = sheet.getSheetName();
        ArrayNode root = mapper.createArrayNode();
        ObjectNode changeSet = mapper.createObjectNode();
        ObjectNode changeSetContent = mapper.createObjectNode();
        ArrayNode changes = mapper.createArrayNode();
        ObjectNode createTable = mapper.createObjectNode();
        ArrayNode _columns = mapper.createArrayNode();
        ObjectNode addPrimaryKey = mapper.createObjectNode();
        ObjectNode addUniqueConstraint = mapper.createObjectNode();
        ObjectNode createIndex = mapper.createObjectNode();
        ObjectNode addForeignKeyConstraint = mapper.createObjectNode();
        
        boolean isAddPk = false;
        boolean isAddUk = false;
        boolean isAddIndex = false;
        boolean isAddFk = false;
        
        String id = Arrays.asList(tableName.toLowerCase().split("_")).stream().map(item -> {
        	if(item.length() > 1)
        		return item.substring(0, 1).toUpperCase()+item.substring(1, item.length());
        	else
        		return item.toUpperCase();
        }).collect(Collectors.joining(""));
        
        id = String.format("create%s-1", id);
        
        changeSetContent.put("id", id);
        changeSetContent.put("author", "iisigroup.com");
        
        //handle columns
        for(Column column : columns) {
        	ObjectNode temp = mapper.createObjectNode();
        	ObjectNode tempColumn = mapper.createObjectNode();
        	temp.put("name", column.getColumn());
        	temp.put("type", column.getType());
        	if(column.getNotNull()!=null) {
        		ObjectNode _temp = mapper.createObjectNode();
        		_temp.put("nullable", "false");
        		temp.putPOJO("constraints", _temp);
        	}
        	tempColumn.putPOJO("column", temp);
        	_columns.add(tempColumn);
        }

        //handle pk
        List<String> pks = new ArrayList<String>();
        for(Column column : columns) {
        	if(column.getPk()!=null)
        		pks.add(column.getColumn());
        }
        String temp = pks.stream().collect(Collectors.joining(","));
        if(temp!=null && !temp.equals("")) {
        	addPrimaryKey.put("columnNames", temp);
        	addPrimaryKey.put("constraintName", "PK_"+tableName);
        	addPrimaryKey.put("tableName", tableName);
        	isAddPk = true;
        }
        
        //handle unique key
        List<String> uks = new ArrayList<String>();
        for(Column column : columns) {
        	if(column.getUk()!=null)
        		uks.add(column.getColumn());
        }        
        String ukStr = uks.stream().collect(Collectors.joining(","));
        if(ukStr!=null && !ukStr.equals("")) {
        	addUniqueConstraint.put("columnNames", ukStr);
        	addUniqueConstraint.put("constraintName", "UK_"+tableName);
        	addUniqueConstraint.put("tableName", tableName);
        	isAddUk = true;
        }
        
        //handle index
        List<String> indexs = new ArrayList<String>();
        for(Column column :  columns) {
        	if(column.getIndex()!=null)
        		indexs.add(column.getColumn());
        }

        if(indexs.size() > 0) {
        	ArrayNode columnsNode = mapper.createArrayNode();
        	for(String column:indexs) {
        		ObjectNode tempNode = mapper.createObjectNode();
        		ObjectNode tempNode2 = mapper.createObjectNode();
        		tempNode2.put("name", column);
        		tempNode.putPOJO("column", tempNode2);
        		columnsNode.add(tempNode);
        	}
        	createIndex.putPOJO("columns", columnsNode);
        	createIndex.put("constraintName", "IDX_"+tableName);
        	createIndex.put("tableName", tableName);
        	isAddIndex = true;
        }
        
        //handle fk
        //{"baseColumnNames":"ROLE_ID","referencedTableName":"SOA_ROLES","referencedColumnNames":"ROLE_ID","onDelete":"RESTRICT","onUpdate":"RESTRICT"}
        List<String> fks = new ArrayList<String>();
        for(Column column : columns) {
        	if(column.getFk()!=null) {
        		fks.add(column.getColumn());
        		ObjectNode tempNode = (ObjectNode) mapper.readTree(column.getFk());
            	addForeignKeyConstraint.put("baseTableName", tableName);
            	addForeignKeyConstraint.put("baseColumnNames", tempNode.get("baseColumnNames").asText());
            	addForeignKeyConstraint.put("constraintName", "FK_" + tableName + "_IN+" + tempNode.get("referencedTableName").asText());
            	addForeignKeyConstraint.put("referencedColumnNames", tempNode.get("referencedColumnNames").asText());
            	addForeignKeyConstraint.put("referencedTableName", tempNode.get("referencedTableName").asText());
            	
            	if(tempNode.get("onDelete") != null) {
            		addForeignKeyConstraint.put("onDelete", tempNode.get("onDelete").asText());
            	}
            	if(tempNode.get("onUpdate") != null) {
            		addForeignKeyConstraint.put("onUpdate", tempNode.get("onUpdate").asText());
            	}
            	isAddFk = true;
        	}
        }        
        
        //marshal
        ObjectNode tempNode;
        createTable.putPOJO("columns", _columns);
        createTable.put("tableName", tableName);
        
        tempNode = mapper.createObjectNode();
        tempNode.putPOJO("createTable", createTable);
        changes.add(tempNode);
        
        changeSetContent.putPOJO("changes", changes);
        
        changeSet.putPOJO("changeSet", changeSetContent);
        
        if(isAddPk) {
        	tempNode = mapper.createObjectNode();
        	tempNode.putPOJO("addPrimaryKey", addPrimaryKey);
        	changes.add(tempNode);
        }
        
        if(isAddUk) {
        	tempNode = mapper.createObjectNode();
        	tempNode.putPOJO("addUniqueConstraint", addUniqueConstraint);
        	changes.add(tempNode);
        }
        
        if(isAddIndex) {
        	tempNode = mapper.createObjectNode();
        	tempNode.putPOJO("createIndex", createIndex);
        	changes.add(tempNode);
        }
        
        if(isAddFk) {
        	tempNode = mapper.createObjectNode();
        	tempNode.putPOJO("addForeignKeyConstraint", addForeignKeyConstraint);
        	changes.add(tempNode);
        }
        
        root.add(changeSet);

        String jsonAsYaml = new YAMLMapper().writeValueAsString(root);
        
        //print yaml
        System.out.println(jsonAsYaml.replace("\"", "").replace("---", ""));
        
        return jsonAsYaml;
	}
}
