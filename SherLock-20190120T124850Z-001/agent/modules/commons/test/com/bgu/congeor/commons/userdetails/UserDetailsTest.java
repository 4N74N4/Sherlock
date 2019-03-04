package com.bgu.congeor.commons.userdetails;

import com.bgu.congeor.UserDetails;
import com.bgu.congeor.commons.SerializationDeserializationTest;

/**
 * Created by clint on 1/5/14.
 */
public class UserDetailsTest extends SerializationDeserializationTest<UserDetails>{
    public UserDetailsTest (){
        super();
        testClass = UserDetails.class;
        ignoredFields.add("phoneID");
        ignoredFields.add("_assignedCoupons");
    }
}
