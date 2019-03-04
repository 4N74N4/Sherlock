package com.bgu.congeor;

import com.bgu.agent.IUserDetails;
import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
//import com.google.code.morphia.annotations.Entity;
//import com.google.code.morphia.annotations.Id;

import java.io.IOException;

/**
 * Created by clint on 1/2/14.
 */

//@Entity("Users")
public class UserDetails implements IUserDetails, KryoSerializable {

    public UserDetails (){

    }

    public UserDetails ( String email ){
        this ( "", email, "", "", 0, "", "", "" + email.hashCode());
    }

    String fullName;
   // @Id
    String email;
    String phoneNumber;
    String facebookToken;
    String gcmRegId = "";
    long facebookTokenExpDate;
    String groupID;
    private String policyID;
    String userID;
    String gender;
    boolean hasVehicle;
    // database specific fields
    String phoneID = "";
    public String expID;

    /*public UserDetails(String fullName, String email, String phoneNumber, String facebookToken, long facebookTokenExpDate, String phoneID, String userID ) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNumber;
        this.facebookToken = facebookToken;
        this.facebookTokenExpDate = facebookTokenExpDate;
        this.gcmRegId = "";
        this.phoneID = phoneID;
        this.userID = userID;
    }*/


    public UserDetails(String fullName, String email, String phoneNum, String facebookToken, long facebookTokenExpDate, String phoneID, String gcmReg, String userID) {
        this.fullName = fullName;
        this.email = email;
        this.phoneNumber = phoneNum;
        this.facebookToken = facebookToken;
        this.facebookTokenExpDate = facebookTokenExpDate;
        this.phoneID = phoneID;
        this.gcmRegId = gcmReg;
        this.userID = userID;
    }

    @Override
    public void write(Kryo kryo, Output output) {
        output.writeString(fullName);
        output.writeString(email);
        output.writeString(phoneNumber);
        output.writeString(gcmRegId);
        output.writeString(facebookToken);
        output.writeLong(facebookTokenExpDate);
        output.writeString(phoneID);
        output.writeString(userID);
    }

    @Override
    public void read(Kryo kryo, Input input) {
        fullName = input.readString();
        email = input.readString();
        phoneNumber = input.readString();
        gcmRegId = input.readString();
        facebookToken = input.readString();
        facebookTokenExpDate = input.readLong();
        try {
            if ( input.available() > 0 )
                phoneID = input.readString();
        } catch (IOException e) {
        }
        try {
            if ( input.available() > 0 )
                userID = input.readString();
        } catch (IOException e) {
            userID = "" + email.hashCode();
        }
    }


    public String getGroupID() {
        return groupID;
    }

    public void setGroupID(String groupID) {
        this.groupID = groupID;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFacebookToken (){
        return facebookToken;
    }

    public long getFacebookTokenExpiration (){
        return facebookTokenExpDate;
    }

    public void setFacebookToken ( String token, long expTimeStamp ){
        this.facebookToken = token;
        this.facebookTokenExpDate = expTimeStamp;
    }

    public String getUserId() {
        return userID;
    }  // TODO ask Clinet why userID

    public String getPhoneID() {
        return phoneID;
    }

    public String getGcmRegId() {
        return gcmRegId;
    }

    public void setGcmRegId(String gcmRegId) {
        this.gcmRegId = gcmRegId;
    }

    public void setPhoneID(String phoneID) {
        this.phoneID = phoneID;
    }

    public String getGender() {
        return gender;
    }

    public boolean isHasVehicle() {
        return hasVehicle;
    }


    @Override
    public String toString (){
        StringBuffer str = new StringBuffer();
        str.append("fullName = " + getFullName());
        str.append("email = " + getEmail());
        str.append("phoneNumber = " + getPhoneNumber());
        str.append("GCMRegID = " + getGcmRegId());
        str.append("FacebookToken = " + getFacebookToken());
        str.append("FacebookExp = " + getFacebookTokenExpiration());
        str.append("phoneID = " + getPhoneID());
        return str.toString();
    }

    public String getPolicyID() {
        return policyID;
    }

    public void setPolicyID(String policyID) {
        this.policyID = policyID;
    }
}