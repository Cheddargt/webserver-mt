/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Project/Maven2/JavaApp/src/main/java/${packagePath}/${mainClassName}.java to edit this template
 */

package com.cheddargt.webserver.mt;

import java.io.*;
import java.net.*;
import java.util.*;

/**
 *
 * @author Gustavo Zeni
 */
public class WebserverMt {

    public static void main(String[] args) throws Exception{
        System.out.println("Server is running...");
        
        // Configura a porta
        int port = 1337;
        // Cria o server socket com a porta definida
        ServerSocket socket = new ServerSocket(port);
        
        //Processa o serviço de requisições HTTP em um loop infinito
        while(true){
            // Listen for a TCP connection request.
            Socket connection = socket.accept();
            
            // Objeto que vai processar a requisição HTTP
            HttpRequest request = new HttpRequest(connection);
            // Cria uma nova thread pra processar a requisição
            Thread thread = new Thread(request);
            // Inicia a thread
            thread.start();
            
            /*
            Quando uma nova thread inicia, a execução da thread principal
            retorna pro topo do loop e aguarda uma nova conexão TCP.
            Enquanto isso, a nova thread continua rodando.
            */
        }
    }
}

final class HttpRequest implements Runnable
{
    // CR (carriage return) LF (line feed) - especificação HTTP
    final static String CRLF = "\r\n";
    // Socket de conexão
    Socket socket;
    
    // Constructor
    public HttpRequest(Socket socket) throws Exception
    {
        this.socket = socket;
    }
    
    // Implement the run() method of the Runnable interface
    // pra passar uma instância de HttpRequest pra construtora de Thread
    public void run()
    {
        try {
            processRequest();
        } catch (Exception e) {
            System.out.println(e);
        }
    }
    
    private void processRequest() throws Exception
    {
        // Get a reference to the socket's input and output streams.
        InputStream is = socket.getInputStream();
        DataOutputStream os = new DataOutputStream(socket.getOutputStream());
        // Set up input stream filters.
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        
        // Get the request line of the HTTP request message.
        // Extrai os caracteres do input stream até chegar no 
        // caractere de fim de linha (CRLF)
        String requestLine = br.readLine();
        
        // Display the request line.
        System.out.println();
        System.out.println(requestLine);
        
        // Get and display the header lines.
        // Usa-se um loop por não saber quantas linhas são.
        String headerLine = null;
        while ((headerLine = br.readLine()).length()!=0) {
            System.out.println(headerLine);
        }

        // Extrai o nome do arquivo da linha de requisição
        StringTokenizer tokens = new StringTokenizer(requestLine);
        tokens.nextToken(); // skip over the method, which should be "GET"
        String fileName = tokens.nextToken();
        
        // Adiciona um "." antes do nome do arquivo pra ficar no formato
        // ./arquivo
        fileName = "." + fileName;

        // Abre o arquivo de requisição.
        FileInputStream fis = null;
        boolean fileExists = true;
        try {
            fis = new FileInputStream(fileName);
        } catch (FileNotFoundException e) {
            fileExists = false;
        }

        // Mensagem de resposta
        String statusLine = null;
        String contentTypeLine = null;
        String entityBody = null;
        
        if (fileExists) {
            statusLine = "HTTP/1.0 200 OK" + CRLF;
            contentTypeLine = "Content-type: " + contentType(fileName) + CRLF;
        } else {
            statusLine = "HTTP/1.0 404 Not Found" + CRLF;
            contentTypeLine = "Content-type: text/html" + CRLF;
            entityBody = "<HTML>" + "<HEAD><TITLE>Not Found</TITLE></HEAD>" + "<BODY>Erro - Arquivo não encontrado</BODY></HTML>";
        }

        // Envia a linha de status.
        os.writeBytes(statusLine);

        // Envia a linha de tipo de conteúdo.
        os.writeBytes(contentTypeLine);

        // Envia a linha em branco pra indicar o fim das linhas de header
        os.writeBytes(CRLF);

        // Envia o body
        if (fileExists) {
            sendBytes(fis, os);
            fis.close();
        } else {
            os.writeBytes(entityBody);
        }

        // Fecha as streams e o socket.
        os.close();
        br.close();
        socket.close();
    }

    private static void sendBytes(FileInputStream fis, OutputStream os) throws Exception
    {
        // Construct a 1K buffer to hold bytes on their way to the socket.
        byte[] buffer = new byte[1024];
        int bytes = 0;

        // Copy requested file into the socket's output stream.
        while((bytes = fis.read(buffer)) != -1 ) {
            os.write(buffer, 0, bytes);
        }
    }

    private static String contentType(String fileName)
    {
        if(fileName.endsWith(".htm") || fileName.endsWith(".html")) {
            return "text/html";
        }
        if(fileName.endsWith(".png")) {
            return "image/png";
        }
        if(fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if(fileName.endsWith(".gif")) {
            return "image/gif";
        }
        return "application/octet-stream";
    }
}
