//package com.paulpetrisor;

import java.net.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;



public class AuctionServer implements Runnable
{  
   
   // Array of clients	
   private AuctionServerThread clients[] = new AuctionServerThread[50];
   private ServerSocket server = null;
   private Thread       thread = null;
   private int clientCount = 0;

   private ArrayList <Item> items = new ArrayList<Item>();
   private Item biddingItem = new Item();
   private int noOfItems = 6;
   private int bidders = 0;
   private boolean bidPlaced = false;
   private static Timer timer;

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
       items.add(new Item("Les Demoiselles d'Avignon", 250));
       items.add(new Item("The Old Guitarist", 50));
       items.add(new Item("The Last Supper", 70));
       items.add(new Item("Las Meninas", 150));
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
   
   public void startAuction(int ID)
   {
      timer = new Timer("Start Auction");
      timer.schedule(new TimerTask() 
      {
            @Override
            public void run() 
            {
               //if a bid was placed. 
               if (bidPlaced && items.size() > 0) {
                  items.remove(0);
                  noOfItems--;
                  System.out.println();
                  System.out.println("NUMBER OF ITEMS LEFT+ " +noOfItems);

                  //notify client who won tha auction and other clients the item was sold.
                  clients[findClient(ID)].send("\n********** You won the item " + "\"" + biddingItem.getName() + "\"" +" for " +  biddingItem.getStartPrice() + "£" + " **********"); 
                  //notify other clients the item was sold 
                  for (int i = 0; i < clientCount; i++)
                  {
                     if(clients[i].getID() != ID)
                     {
                        clients[i].send("\nThe item " + "\"" + biddingItem.getName() + "\""  + "was sold for " + biddingItem.getStartPrice() + "£");
                     }
                  }
               
                  //Notify all client there are no more items on sale for this auction/
                  if (items.size() == 0) 
                  {
                     for (int i = 0; i < clientCount; i++)
                     {
                        clients[i].send("\nThe auction is over. \nNo more items available today. \nSee you again soon");
                     }
                     biddingItem = null;
//                     System.exit(0);
                    
                  }
                  else {
                     getNextItemForBid();
                     for (int i = 0; i < clientCount; i++)
                     {
                        clients[i].send("\nNext item on auction is " + biddingItem.getName() + " and starting price is " + biddingItem.getStartPrice() + "£");
                     }
                  }
                  //reset bidPlace to false for nect item , cancel the timer and start auction again for next item.
                  bidPlaced = false;
                  timer.cancel();
                  startAuction(ID);
            
               }//end if(bidPlaced)
               
               else {
                  //if statement for error control when array is empty
                  if (items.size() > 0) {
                     for (int i = 0; i < clientCount; i++)
                     {
                        clients[i].send("\nThe item " + biddingItem.getName() + " was not sold. Will be relisted later.");
                     }
                     items.remove(0);
                     items.add(new Item(biddingItem.getName(), biddingItem.getStartPrice()));
                     getNextItemForBid();
                     for (int i = 0; i < clientCount; i++)
                     {
                        clients[i].send("\nNext item on auction is " + biddingItem.getName() + " and starting price is " + biddingItem.getStartPrice() + "£");
                     }
                     System.out.println("\nIn startAcution no bid placed");
                     timer.cancel();
                     startAuction(ID);
                  }
               }
            }
      },60000);
      
      timer.schedule(new TimerTask() {

         @Override
         public void run()
         {
            for (int i = 0; i < clientCount; i++)
            {
               clients[i].send("\n30 seconds left to bid for this item!!!");             
            }

         }
      }, 30000);
      
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
		  clients[findClient(ID)].send("Goodbye");
          remove(ID);
       }
       else
      {
         int usersBid = 0;
         //client input validation
         try
         {
               if(input != null)
                   usersBid = Integer.parseInt(input);
         }
         catch (NumberFormatException e)
         {
               usersBid = 0;
         }
         
         if(items.size() > 0 && usersBid > biddingItem.getStartPrice()) //check if any items are left and if usersBid is greater    than startPrice
         {
               biddingItem.setNewPrice(usersBid);
            
            for (int i = 0; i < clientCount; i++)
            {
   			   if(clients[i].getID() != ID)
               {	clients[i].send("\nNew higest value for " + biddingItem.getName() + " is " + biddingItem.getStartPrice() + "£"); // sends messages to clients
               }	
            }	
               clients[findClient(ID)].send("\nYou are the highest bidder for " + biddingItem.getName() + " with "  + biddingItem.getStartPrice() + "£"); 
               //set boolean variable true if a valid bid has been accepted reset timer and dtart auction logic
               bidPlaced = true;
               timer.cancel(); //reset timer 
               startAuction(ID);
           // }//end for loop
         }//end inner if 
         
         else 
         { 
            clients[findClient(ID)].send("\n!!!!!!!!!!! A valid bid must be higher than the current value of " + biddingItem.getStartPrice() + "£ and less than " + Integer.MAX_VALUE + ", value of a integer !!!!!!!!!!"); //if bid value is lower or too high
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
            welcome(clients[clientCount-1].getID()); //send the appropiate welcome message on thread creation to each client.
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
   
   
   //function used to display message to the client when joining the auction
   public void welcome(int id)
   {
      // if first client wait for a second to coonect
       if(clientCount == 1 && noOfItems > 0)
       {
            clients[findClient(id)].send("\nWelcome to my auction. You are the first bidder, waiting for another before we can start!");
            System.out.println("in welcome if client==1");
       }
       //auction can start
       else if (clientCount == 2 && noOfItems > 0)
       {
           clients[findClient(id)].send("\n********** Welcome to the auction. Auction is starting now. **********");

           for (int i = 0; i < clientCount; i++)
           {
                clients[i].send("\n\t\t\t********** Auction started!!! **********");
                clients[i].send("\nItem on sale is " + biddingItem.getName() + " and starting price is " + biddingItem.getStartPrice() + "£");
           }
          startAuction(id);
       }
      else if (clientCount > 2 && noOfItems > 0)
      {
         clients[findClient(id)].send("\n********** Welcome to the auction. Bidding is running now. **********");
         clients[findClient(id)].send("\nItem on sale is " + biddingItem.getName() + " and the price is " + biddingItem.getStartPrice() + "£");
      }
      
      else {
            clients[findClient(id)].send("\n Auction is over. Come back tommorrow for a new round");
            //remove(id);
            System.exit(0); //Server will be stopped if no items left so the client wont be able to join 
      }
   }

   public static void main(String args[]) {
	   AuctionServer server = null;
      if (args.length != 1)
         System.out.println("Usage: java AuctionServer port");
      else
         server = new AuctionServer(Integer.parseInt(args[0]));
   }

}