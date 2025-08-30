import java.util.*;
import java.net.*;
import java.io.*;
import java.nio.file.*;

class Main
{
	// browser identifies the type of file sent in response
	private static String getMimeType(String path) 
	{
    	if (path.endsWith(".html")) return "text/html";
    	if (path.endsWith(".css")) return "text/css";
    	if (path.endsWith(".js")) return "application/javascript";
    	if (path.endsWith(".png")) return "image/png";
    	if (path.endsWith(".jpg") || path.endsWith(".jpeg")) return "image/jpeg";
    	if (path.endsWith(".gif")) return "image/gif";
    	if (path.endsWith(".ico")) return "image/x-icon";
    	return "text/plain";
	}	
 

	public static void main(String args[])
	{
		int portNo = 8080;

		try 
		{
			InetAddress ip = InetAddress.getByName("192.168.201.94");
			ServerSocket server = new ServerSocket(portNo,50,ip);

			System.out.println("HTTP Server started on port: " + portNo);

			while(true)
			{
				Socket clientSock = server.accept();
				System.out.println("New Connection from " + clientSock.getInetAddress());

				BufferedReader FromClient = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
				OutputStream toClient = clientSock.getOutputStream();

				String clientRequest = FromClient.readLine();
				if(clientRequest == null)
				{
					clientSock.close();
					continue;
				}

				System.out.println("Request Message: " + clientRequest);

				String[] tokens = clientRequest.split(" ");
				String method = tokens[0]; //GET ot POST
				String path = tokens[1];

				String HTTPResponse = "";

				// handling /,/about,/login

				if(path.equals("/"))
				{
					path = "/index.html";
				}
				else if(path.equals("/about"))
				{
					path = "/about.html";
				}
				else if(path.equals("/login"))
				{
					path = "/login.html";
				}
				else if(path.equals("/favicon.ico"))
				{
					path = "/favicon.ico";
				} 

				// this processes everything in bytes instead of text, allows you to read images
				File file = new File("www" + path);
                if (file.exists() && !file.isDirectory()) 
                {
                    byte[] fileBytes = Files.readAllBytes(file.toPath());
                    String header = "HTTP/1.1 200 OK\r\n" +
                            "Content-Type: " + getMimeType(path) + "\r\n" +
                            "Content-Length: " + fileBytes.length + "\r\n\r\n";
                    toClient.write(header.getBytes());
                    toClient.write(fileBytes); // send actual file content
                } 
                else 
                {
                    // 404 Page Not Found
                    String error = "<h1>404 Page Not Found</h1>";
                    String header = "HTTP/1.1 404 Not Found\r\n" +
                            "Content-Type: text/html\r\n" +
                            "Content-Length: " + error.length() + "\r\n\r\n";
                    toClient.write(header.getBytes());
                    toClient.write(error.getBytes()); 
				
                }

                // server_logs

                try 
                {
                	FileWriter fw = new FileWriter("server_logs.txt", true);
     				BufferedWriter bw = new BufferedWriter(fw);
     				PrintWriter out = new PrintWriter(bw);
    				String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
					out.println(timestamp + " | " + clientSock.getInetAddress() + " | " + clientRequest + " | File exists: " + file.exists());
					out.flush();
					out.close();
					bw.close();
					fw.close();

				} 
				catch (IOException e) 
				{
    				e.printStackTrace();
				}


				toClient.flush();
				clientSock.close();
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}