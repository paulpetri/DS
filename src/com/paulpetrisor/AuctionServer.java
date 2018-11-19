//package com.paulpetrisor;

import java.net.*;
import java.io.*;
import java.util.ArrayList;import java.time.*;



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
   private boolean bidPlaced = false;
   Instant t1 , t2;

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
       items.add(new Item("MonaLisa", 100));
       items.add(new Item("Girl Before A Mirror", 25));
       items.add(new Item("Les Demoiselles d'Avignon", 150));
       items.add(new Item("The Old Guitarist", 50));
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
   
   public void startAuction()
   {
//      @Override
  //       public void run() {
         if (bidPlaced && items.size() > 2) {
            //remove first item and add it to the auction
            
            //getNextItemForBid();
            System.out.println("bidders == 0");
//            long ns = Duration.between(t1, t2).toNanos();
//            System.out.println(ns);
             for (int i = 0; i < clientCount; i++)
               {
               clients[i].send("Current item on auction is " + biddingItem.getName() + " and curent price is " + biddingItem.getStartPrice() + "£");
               }
               
            items.remove(0);
            getNextItemForBid();
            
            for (int i = 0; i < clientCount; i++)
            {
               clients[i].send("Next item on auction is " + biddingItem.getName() + " and starting price is " + biddingItem.getStartPrice() + "£");
            }
            bidPlaced = false;
            startAuction();
            
        }//end if(bidPlaced)
//      }
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
      {
         int usersBid = 0;
         try
         {
               if(input != null)
                   usersBid = Integer.parseInt(input);
         }
         catch (NumberFormatException e)
         {
               usersBid = 0;
         }
         //int usersBid = Integer.parseInt(input);
         
         if(items.size() > 0 && usersBid > biddingItem.getStartPrice()) //check if any items are left and if usersBid is greater    than price
         {
               biddingItem.setNewPrice(usersBid);
            
            for (int i = 0; i < clientCount; i++)
            {
   			   if(clients[i].getID() != ID)
               {	clients[i].send("New higest value for " + biddingItem.getName() + " is " + biddingItem.getStartPrice() + "£"); // sends messages to clients
               }	
            }	
               clients[findClient(ID)].send("\nYou are the highest bidder " + biddingItem.getStartPrice() + " for " + biddingItem.getName()); 
               //set boolean variable true if a valid bid has been accepted
               bidPlaced = true;
               startAuction();
           // }//end for loop
         }//end inner if 
         else 
         { 
            clients[findClient(ID)].send("A valid bid must be higher than the current value of " + biddingItem.getStartPrice() + "£"); //if bid value is lower
         }
      }//end outer else 
      
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
       else if (clientCount >= 2 && noOfItems > 0)
       {
           clients[findClient(id)].send("Welcome to the auction. Bidding is underway.");
           System.out.println("in welcome 22");
           for (int i = 0; i < clientCount; i++)
           {
                clients[i].send("Item on sale is " + biddingItem.getName() + " and starting price is " + biddingItem.getStartPrice());
           }
          startAuction();
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