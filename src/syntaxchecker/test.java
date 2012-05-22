/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package syntaxchecker;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author snadi
 */
public class test {

    public static void main(String[] args){
                 Pattern pattern2 = Pattern.compile("[^a-zA-Z0-9_-]usbhid.o(\\s*|\\n)");
                Matcher matcher = pattern2.matcher(" 	usbhid-objs	+= hid-pidff.o");
                System.out.println(matcher.find());

    }

}
