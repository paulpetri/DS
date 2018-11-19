//package com.paulpetrisor;


public class Item {
    private String name;
    private int startPrice;

    public Item()
    {

    }
    public Item(String iName, int price) {
        name = iName;
        startPrice = price;
    }

    public String getName() {
        return name;
    }

    public int getStartPrice() {
        return startPrice;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNewPrice(int newPrice)
    {
        this.startPrice = newPrice;
    }

}