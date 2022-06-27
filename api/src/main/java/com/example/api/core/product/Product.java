package com.example.api.core.product;

public class Product {
  private int productId;
  private String name;
  private int weight;
  private String serviceAddress;

  public Product() {
    productId = 0;
    name = null;
    weight = 0;
    serviceAddress = null;
  }

  public Product(int productId, String name, int weight, String serviceAdress) {
    this.productId = productId;
    this.name = name;
    this.weight = weight;
    this.serviceAddress = serviceAdress;
  }

  public int getProductId() {
    return productId;
  }

  public String getName() {
    return name;
  }

  public int getWeight() {
    return weight;
  }

  public String getServiceAddress() {
    return serviceAddress;
  }

  public void setServiceAddress(String newServiceAddress) {
    this.serviceAddress = newServiceAddress;
  }

  public void setProductId(int productId) {
    this.productId = productId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setWeight(int weight) {
    this.weight = weight;
  }
}
