package com.hua.rpc.client;

import java.util.List;

public interface PersonService {
    List<Person> GenTestPerson(String name, int num);
}
