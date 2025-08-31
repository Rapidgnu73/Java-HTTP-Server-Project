import java.util.*;
import java.net.*;
import java.io.*;
import java.nio.file.*;
import java.util.concurrent.*;

class ClientHandler implements Runnable
{
	private Socket clientSock;
	ClientHandler(Socket sock)
	{
		this.clientSock = sock;
	}
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

	private static boolean isAlphaNum(char a)
	{
		if(('a'<=a && a<='z') || ('A'<=a && a<='Z') || ('0'<=a && a<='9'))
			return true;
		return false;
	}
	public void run() 
	{
		try
		{
			System.out.println("New Connection from " + clientSock.getInetAddress());

			BufferedReader FromClient = new BufferedReader(new InputStreamReader(clientSock.getInputStream()));
			OutputStream toClient = clientSock.getOutputStream();

			String clientRequest = FromClient.readLine();
			if(clientRequest == null)
			{
				clientSock.close();
				return;
			}

			System.out.println("Request Message: " + clientRequest);

			String[] tokens = clientRequest.split(" "); // only the first line of the client req
			String method = tokens[0]; //GET or POST
			String path = tokens[1];

			String HTTPResponse = "";

			File file;

			// handling the POST http request
			// currently handles only application/x-www-form-urlencoded type of content
			if(method.equals("POST"))
			{
				HashMap<String,String> headers = new HashMap<String,String>();
				String line = FromClient.readLine();
				while(line!=null && !line.equals(""))
				{
					String[] headerContent = line.split(":",2);
					if(headerContent.length == 2)
						headers.put(headerContent[0].trim(),headerContent[1].trim());
					line = FromClient.readLine();
				}
				int len = Integer.parseInt(headers.getOrDefault("Content-Length","0"));
				char[] b = new char[len];
				FromClient.read(b,0,len);
				String body = new String(b);

				HashMap<String,String> bodyKV = new HashMap<String,String>();
				
				for(String keyValue:body.split("&"))
				{
					String[] pair = keyValue.split("=",2);
					if(pair.length == 2)
					{
						String key = URLDecoder.decode(pair[0],"UTF-8");
						String value = URLDecoder.decode(pair[1],"UTF-8");
						bodyKV.put(key,value);
					}
				}
				// store the data onto a CSV file;
				FileWriter saveCSV = new FileWriter("FormsSubmissions.csv",true);
				PrintWriter pw = new PrintWriter(saveCSV);
				String data = bodyKV.getOrDefault("name"," ") + "," + bodyKV.getOrDefault("email"," ") + "," +
				bodyKV.getOrDefault("dob"," ") + "," + 
				bodyKV.getOrDefault("message"," ") + "\n";
				pw.println(data);
				pw.close();

				// thank you page after reading the post request
				file = new File("www/thankyou.html");
				if(file.exists() && !file.isDirectory())
				{
					byte[] fileBytes = Files.readAllBytes(file.toPath());
					String H = "HTTP/1.1 200 OK\r\n" +
       	    		"Content-Type: " + getMimeType("/thankyou.html") + "\r\n" +
            		"Content-Length: " + fileBytes.length + "\r\n\r\n";
            		toClient.write(H.getBytes());
            		toClient.write(fileBytes);
				}
				else
				{
					String error = "<h1>404 Page Not Found</h1>";
            		String header = "HTTP/1.1 404 Not Found\r\n" +
            		"Content-Type: text/html\r\n" +
            		"Content-Length: " + error.length() + "\r\n\r\n";
            		toClient.write(header.getBytes());
            		toClient.write(error.getBytes());
				}

			}
			else
			{
				if(path.equals("/"))
				{
					path = "/index.html";
				}
				else if(path.equals("/about"))
				{
					path = "/about.html";
				}
				else if(path.equals("/info"))
				{
					path = "/info.html";
				}
				else if(path.equals("/forms.html"))
				{
					path = "/forms.html";
				}
				else if(path.equals("/favicon.ico"))
				{	
					path = "/favicon.ico";
				} 

			// this processes everything in bytes instead of text, allows you to read images
				file = new File("www" + path);
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
        	}

        // server_logs

            FileWriter fw = new FileWriter("server_logs.txt", true);
     		BufferedWriter bw = new BufferedWriter(fw);
     		PrintWriter out = new PrintWriter(bw);
    		String timestamp = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new java.util.Date());
			out.println(timestamp + " | " + clientSock.getInetAddress() + " | " + clientRequest + " | File exists: " + file.exists());
			out.flush();
			out.close();
			bw.close();
			fw.close();

			toClient.flush();
			clientSock.close();
		}
		catch (IOException e) 
		{
    		e.printStackTrace();
		}

	}
}

class Main
{	

	public static void main(String args[])
	{
		int portNo = 8080;

		try 
		{
			InetAddress ip = InetAddress.getByName("10.86.1.76");
			ServerSocket server = new ServerSocket(portNo,50,ip);

			System.out.println("HTTP Server started on port: " + portNo);

			ExecutorService threadpool = Executors.newFixedThreadPool(50);

			while(true)
			{
				Socket clientSock = server.accept();
				threadpool.execute(new ClientHandler(clientSock));	
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}