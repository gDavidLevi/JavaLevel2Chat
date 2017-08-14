package ru.davidlevy.server;

import java.util.Calendar;

public class _test {

    public static void main(String[] args) {
        //System.out.println(java.sql.Date.valueOf(java.time.LocalDate.now()));
       // System.out.println(java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(1)));
        //
        Calendar calendar = Calendar.getInstance();
        java.sql.Timestamp ourJavaTimestampObject = new java.sql.Timestamp(calendar.getTime().getTime());
        System.out.println(ourJavaTimestampObject);
        //
        java.sql.Timestamp one = new java.sql.Timestamp(java.sql.Date.valueOf(java.time.LocalDate.now().minusDays(10)).getTime());
        System.out.println(one);

    }
}
