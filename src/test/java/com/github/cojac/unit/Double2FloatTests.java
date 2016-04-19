/*
 * *
 *    Copyright 2011-2016 Valentin Gazzola & Frédéric Bapst
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */
package com.github.cojac.unit;


public class Double2FloatTests {

    public double testNextUp() {
        double a = Math.nextUp(3) , b = 0;
        double c = a + b;
        //expectedResults.add(3.0f-Math.ulp(3.0f));
        return c;
    }
    public double testPrecision(){
        double a = 0.1f;
        double b = 0;
        
        for (int i = 0; i < 10; i++) {
            b+=a;
        }
        return b;
    }
    
}