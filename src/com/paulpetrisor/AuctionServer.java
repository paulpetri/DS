//package com.paulpetrisor;

import java.net.*;
import java.io.*;
import java.util.ArrayList;


public class AuctionServer implements Runnable
{  
   
   // Array of clients	
   private AuctionServerThread clients[] = new AuctionServerThread[50];
   private ServerSocket server = null;
   private Thread       thread = null;
   private int clientCount = 0;

   private ArrayList <Item> items = new ArrayList<Item>();
   private Item biddingItem = new Item();
   private int noOfItems = 4;
   private int bidders = 0;

   public AuctionServer(int port)
   {
	  try {

		 System.out.println("Binding to port " + port + ", please wait  ...");
         server = new ServerSocket(port);
         System.out.println("Server started: " + server.getInetAddress());
         start();
         addItems();
         getNextItemForBid();
      }
      catch(IOException ioe)
      {
		  System.out.println("Can not bind to port " + port + ": " + ioe.getMessage());

      }
   }

   public void addItems()
   {
       items.add(new Item("MonaLisa", 100000));
       items.add(new Item("Girl Before A Mirror", 25000));
       items.add(new Item("Les Demoiselles d'Avignon", 15000));
       items.add(new Item("The Old Guitarist", 50000));
   }

   public void getNextItemForBid()
   {
       biddingItem = new Item(items.get(0).getName(), items.get(0).getStartPrice());
   }

   public void run()
   {
	  while (thread != null)
      {
		 try{

			System.out.println("Waiting for a client ...");
            addThread(server.accept());

			int pause = (int)(Math.random()*3000);
			Thread.sleep(pause);

         }
         catch(IOException ioe){
			System.out.println("Server accept error: " + ioe);
			stop();
         }
         catch (InterruptedException e){
		 	System.out.println(e);
		 }
      }
   }
   
   public void runAuction()
   {
//      @Override
//      public void run() {
         if (bidders == 0) {
            //remove first item and add it to the auction
            items.remove(0);
            getNextItemForBid();
            System.out.println("bidders == 0");
//         }
      }
   }

  public void start()
    {
		if (thread == null) {
		  thread = new Thread(this);
          thread.start();
       }
    }

   public void stop(){
	   thread = null;

   }

   private int findClient(int ID)
   {
	   for (int i = 0; i < clientCount; i++)
         if (clients[i].getID() == ID)
            return i;
      return -1;
   }

   public synchronized void broadcast(int ID, String input)
   {
	   if (input.equals(".bye")){
		  clients[findClient(ID)].send(".bye");
          remove(ID);
       }
       else
         for (int i = 0; i < clientCount; i++){
			if(clients[i].getID() != ID)
            	clients[i].send(ID + ": " + input); // sends messages to clients
		   }
      runAuction();
      notifyAll();
   }
   public synchronized void remove(int ID)
   {
	  int pos = findClient(ID);
      if (pos >= 0){
		 AuctionServerThread toTerminate = clients[pos];
         System.out.println("Removing client thread " + ID + " at " + pos);

         if (pos < clientCount-1)
            for (int i = pos+1; i < clientCount; i++)
               clients[i-1] = clients[i];
         clientCount--;

         try{
			 toTerminate.close();
	     }
         catch(IOException ioe)
         {
			 System.out.println("Error closing thread: " + ioe);
		 }
		 toTerminate = null;
		 System.out.println("Client " + pos + " removed");
		 notifyAll();
      }
   }

   private void addThread(Socket socket)
   {
	  if (clientCount < clients.length){

		 System.out.println("Client accepted: " + socket);
         clients[clientCount] = new AuctionServerThread(this, socket);
         try{
			clients[clientCount].open();
            clients[clientCount].start();

            System.out.println(clients[clientCount].getID());
            System.out.println("In addThread");
            clientCount++;
            welcome(clients[clientCount-1].getID());
            System.out.println(clientCount);
            System.out.println(noOfItems);
         }
         catch(IOException ioe){
			 System.out.println("Error opening thread: " + ioe);
		  }
	  }
      else
         System.out.println("Client refused: maximum " + clients.length + " reached.");
   }
   //display message to the client when joining the auction
   public void welcome(int id)
   {
      // if first client wait for a second to coonect
       if(clientCount == 1 && noOfItems > 0)
       {
            clients[findClient(id)].send("Welcome to my auction. You are the first bidder, waiting for another before we can start!");
            System.out.println("in welcome if client==1");
       }
       //auction can start
       else if (clientCount == 2 && noOfItems > 0)
       {
           clients[findClient(id)].send("Welcome to the auction. Bidding will start soon.");
           System.out.println("in welcome 22");
           for (int i = 0; i < clientCount; i++)
           {
                clients[i].send("Next item on sales is " + biddingItem.getName() + " and starting price is " + biddingItem.getStartPrice());
           }
       }
   }

   public static void main(String args[]) {
	   AuctionServer server = null;
//      if (args.length != 1)
//         System.out.println("Usage: java AuctionServer port");
//      else
         server = new AuctionServer(123);//Integer.parseInt(args[0]));
   }

}