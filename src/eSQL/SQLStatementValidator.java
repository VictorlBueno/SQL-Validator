package eSQL;

import javax.swing.JFileChooser;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.JFrame;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.atomic.AtomicInteger;

public class SQLStatementValidator {
    private static StringBuilder errors = new StringBuilder();
    private static MessageBox messageBox;
    private static boolean windowClosed = false;
    private static final int BATCH_SIZE = 5000; // Tamanho do lote para processamento
    public static String errorText;

    public static void main(String[] args) {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(null);   

        // Verifica se um arquivo foi selecionado corretamente
        if (result == JFileChooser.APPROVE_OPTION) {
            messageBox = new MessageBox();
            JFrame frame = (JFrame) messageBox.getWindow();
            frame.addWindowListener(new java.awt.event.WindowAdapter() {
                @Override
                public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                    windowClosed = true;
                    messageBox.dispose();
                    System.exit(0); // Finaliza o programa quando a janela é fechada
                }
            });
            String filename = fileChooser.getSelectedFile().getAbsolutePath();

            try (BufferedReader br = new BufferedReader(new FileReader(filename))) {
                
                AtomicInteger errorsNum = new AtomicInteger(0);
                ExecutorService executorService = Executors.newCachedThreadPool();
                final AtomicInteger currentErrorsNum = errorsNum;
                
                String line;
                int counter = 0;
                
                while (!windowClosed && (line = br.readLine()) != null) {
                    counter++;
                    final String currentLine = line;
                    
                    messageBox.loading(counter, errorsNum.get());

                    executorService.execute(() -> {
                        boolean validationResult = validateSQLStatement(currentLine);

                        if (!validationResult) {
                            synchronized (errors) {
                                errors.append("&nbsp;&nbsp;&nbsp;&nbsp;" + errorText + ": ").append(currentLine).append("<br>");
                                System.out.println(errorText + ": " + currentLine);
                                currentErrorsNum.incrementAndGet();
                            }
                        }
                    });

                    // Verifica se atingiu o tamanho do lote
                    if (counter % BATCH_SIZE == 0) {
                    	// Aguarda o processamento do lote atual ser concluído antes de prosseguir para o próximo
                    	executorService.shutdown();
                    	executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                        // Verifica se a janela foi fechada
                        if (windowClosed) {
                            break;
                        }

                        // Cria um novo executor para processar o próximo lote
                        executorService = Executors.newCachedThreadPool();
                    }
                }

                // Processa o último lote (se houver)
                if (!windowClosed) {
                    executorService.shutdown();
                    executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
                }

                if (errors.length() > 0) {
                    messageBox.showDetails(errors.toString(), errorsNum.get());
                } else {
                    messageBox.showDetails("", errorsNum.get());
                }
            } catch (IOException e) {
                System.err.println("Erro ao ler o arquivo: " + e.getMessage());
            } catch (InterruptedException e) {
                System.err.println("Erro ao aguardar a conclusão das threads: " + e.getMessage());
            }
        }
    }
    // Valida um comando SQL e retorna o resultado da validação.
    private static boolean validateSQLStatement(String line) {
        if (line.toUpperCase().contains("N/A")) {
        	errorText = "Invalid data";
            return false;
        } else if (line.toUpperCase().contains("INSERT")) {
            return isInsertStatement(line);
        } else if (line.toUpperCase().contains("DELETE")) {
            return isDeleteStatement(line);
        } else if (line.toUpperCase().contains("UPDATE")) {
            return isUpdateStatement(line);
        } else if (line.contains("------")) {
            return true;
        } else {
        	errorText = "Syntax error";
            return false;
        }
    }

    // Verifica se um comando SQL é um comando de inserção válido.
    private static boolean isInsertStatement(String line) {
    	line = line.replaceAll("\\s+", " ");
    	
        int commas = 0;
        int parentheses = 0;
        boolean quotation = false;

        // Conta vírgulas que não estão dentro de aspas
        for (int i = 0; i < line.length(); i++) {
            if (quotation == false && line.charAt(i) == '"') {
                quotation = true;
            } else if (quotation == true && line.charAt(i) == '"' && line.charAt(i+1) == ',' 
            		|| quotation == true && line.charAt(i) == '"' && line.charAt(i+2) == ','
            		|| quotation == true && line.charAt(i) == '"' && line.charAt(i+1) == ')'
            		|| quotation == true && line.charAt(i) == '"' && line.charAt(i+2) == ')') {
                quotation = false;
            }
            if (quotation == false && line.charAt(i) == ',') {
            	commas++;
            }
            if (quotation == false && line.charAt(i) == '(' || quotation == false && line.charAt(i) == ')') {
            	parentheses++;
            }
        }
        
        // Se utilizar aspas simples ao invés de dupla
        if(commas % 2 != 0 || commas == 0) {
            commas = 0;
            parentheses = 0;
            quotation = false;
            
            // Conta vírgulas que não estão dentro de aspas
            for (int i = 0; i < line.length(); i++) {
                if (quotation == false && line.charAt(i) == '\'') {
                    quotation = true;
                } else if (quotation == true && line.charAt(i) == '\'' && line.charAt(i+1) == ','  
                		|| quotation == true && line.charAt(i) == '\'' && line.charAt(i+2) == ','
                		|| quotation == true && line.charAt(i) == '\'' && line.charAt(i+1) == ')'
                		|| quotation == true && line.charAt(i) == '\'' && line.charAt(i+2) == ')') {
                    quotation = false;
                }
                if (quotation == false && line.charAt(i) == ',') {
                	commas++;
                }
                if (quotation == false &&  line.charAt(i) == '(' || quotation == false && line.charAt(i) == ')') {
                	parentheses++;
                }
            }
        }

        // Verifica pariedade de parenteses
        if(quotation == true) {
        	errorText = "Unclosed quote";
        	return false;
        } else if(parentheses % 2 != 0) {
        	errorText = "Syntax error";
        	return false;
        }
        
        if(parentheses == 2) {
            // Quantidade de colunas
            String valuesPattern = ".*";
            for (int i = 0; i < commas; i++) {
                valuesPattern += ", .*";
            }
            
            String insertPattern = "INSERT INTO [^,]* VALUES (" + valuesPattern + ");";
            Pattern pattern = Pattern.compile(insertPattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(line);
            
            if(!matcher.matches()) {
                insertPattern = "INSERT INTO [^,]* VALUES(" + valuesPattern + ");";
                pattern = Pattern.compile(insertPattern, Pattern.CASE_INSENSITIVE);
                matcher = pattern.matcher(line);
            }
            
            if(!matcher.matches()) {
            	errorText = "Syntax error";
            	return false;
            }
            
        } else if(parentheses == 4) {
        	commas = commas / 2;
        	
            // Quantidade de colunas
            String valuesPattern = ".*";
            for (int i = 0; i < commas; i++) {
                valuesPattern += ", .*";
            }
            
            String insertPattern = "INSERT INTO [^,]*(" + valuesPattern + ") VALUES (" + valuesPattern + ");";
            Pattern pattern = Pattern.compile(insertPattern, Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(line);
            
            if(!matcher.matches()) {
                insertPattern = "INSERT INTO [^,]*(" + valuesPattern + ") VALUES(" + valuesPattern + ");";
                pattern = Pattern.compile(insertPattern, Pattern.CASE_INSENSITIVE);
                matcher = pattern.matcher(line);
            }
            
            if(!matcher.matches()) {
            	errorText = "Syntax error";
            	return false;
            	
            } else if(commas % 2 != 0) {
            	errorText = "Columns and values don't match";
            	return false;
            }
            
        } else {
        	errorText = "Parentheses don't match";
        	return false;
        }
        
        
        return true;
    }

    private static boolean isDeleteStatement(String line) {
        String deletePattern = "DELETE FROM [^,]*;";
        Pattern pattern = Pattern.compile(deletePattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line.toUpperCase());

        if (!matcher.matches()) {
            deletePattern = "DELETE FROM [^,]* WHERE [^,]*=[^,]*;";
            pattern = Pattern.compile(deletePattern, Pattern.CASE_INSENSITIVE);
            matcher = pattern.matcher(line);
        }

        if(matcher.matches() == false) {
        	errorText = "Syntax error";
        }
        return matcher.matches();
    }

    private static boolean isUpdateStatement(String line) {
        String updatePattern = "UPDATE [^,]* SET .* WHERE [^,]*;";
        Pattern pattern = Pattern.compile(updatePattern, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(line);
        if(matcher.matches() == false) {
        	errorText = "Syntax error";
        }
        return matcher.matches();
    }
}