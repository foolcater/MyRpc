package com.hua.rpc.server;

import com.hua.rpc.annotation.RpcService;
import com.hua.rpc.client.Person;
import com.hua.rpc.client.PersonService;

import java.util.ArrayList;
import java.util.List;

@RpcService(PersonService.class)
public class PersonServiceImpl implements PersonService{

    @Override
    public List<Person> GenTestPerson(String name, int num) {
        List<Person> persons = new ArrayList<Person>(num);
        for (int i=0; i<num; i++){
            persons.add(new Person(Integer.toString(i), name));
        }
        return persons;
    }
}
