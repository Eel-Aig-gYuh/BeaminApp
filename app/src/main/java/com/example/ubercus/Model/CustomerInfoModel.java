package com.example.ubercus.Model;

public class CustomerInfoModel {
    private String firstName, lastName, phoneNumber, avatar;
    private double rating;


    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public double getRating() {
        return rating;
    }

    public CustomerInfoModel(){

    }
}