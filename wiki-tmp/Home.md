﻿<p align="center">
<img src="https://github.com/Cojac/Cojac/wiki/images/logo-cojac-512.png"
   width=256" />
<h1 align="center">********** User Guide **********</h1>
</p>

1. [Introduction](home#1---introduction)

  1. [Overview](home#11---overview)
  2. [Launching an application with COJAC](home#12---launching-an-application-with-cojac)
  3. [Links and notes](home#13---links-and-notes)
  
2. [COJAC: the numerical sniffer](home#2---cojac-the-numerical-sniffer)
  
  1. [What COJAC considers suspicious](home#21---what-cojac-considers-suspicious)
  2. [Configuring what COJAC will detect](home#22---configuring-what-cojac-will-detect)
  3. [Configuring how COJAC will signal the detected problems](home#23---configuring-how-cojac-will-signal-the-detected-problems)
  4. [Instrumenting without executing](home#24---instrumenting-without-executing)
  5. [Example](home#25---example)
  6. [JMX feature](home#26---jmx-feature)
  
3. [COJAC: the enriching wrapper](home#3---cojac-the-enriching-wrapper)

  1. [Our wrapping mechanism](home#31---our-wrapping-mechanism)
  2. [Number model "BigDecimal"](home#32---number-model-bigdecimal)
  3. [Number model "Interval computation"](home#33---number-model-interval-computation)
  4. [Number model "Discrete stochastic arithmetic"](home#34---number-model-discrete-stochastic-arithmetic)
  5. [Number model "Automatic differentiation"](home#35---number-model-automatic-differentiation)
  6. [Number model "Symbolic computation"](home#36---number-model-symbolic-computation)
  7. [Number model "Chebfun"](home#37---number-model-chebfun)

4. [COJAC: Changing Java's primitive type behaviour](home#4---Changing-javas-primitive-type-behaviour)

5. [Detailed usage](home#5---detailed-usage)

6. [Limitations and known issues](home#6---limitations-and-known-issues)

  1. [Issues with the sniffer](home#61---issues-with-the-sniffer)
  2. [Issues with the wrapper](home#62---issues-with-the-wrapper)

7. [And now...](home#7---and-now)

--------------------------------------------------
# 1 - Introduction

Welcome to COJAC, a tool that leverages in new ways the arithmetic 
capabilities of the Java programming language. The idea is summarized in 
small demos on [YouTube](https://youtu.be/DqAFQfbWZOU?list=PLHLKWUtT0B7kNos1e48vKhFlGAXR1AAkF).

If you need a definition for the acronym "COJAC", feel free to choose one of 
following:

- `C`limbing `O`ver `J`ava `A`lgebraic `C`omputation
- `C`reating `O`ther `J`uicy `A`rithmetic `C`apabilities
- `C`hecking `O`verflows in `JA`va `C`ode

## 1.1 - Overview
COJAC is in fact a two-fold tool: 

- a **_"numerical sniffer"_** that detects anomalies arising in arithmetic operations, 
on both integers (eg overflow) and floating point numbers (eg 
cancellation or NaN outcome). This can be of great help to detect vicious bugs 
involving annoying events that normally happen silently in the Java Virtual 
Machine. See [§2](home#2---cojac-the-numerical-sniffer). This tool is pretty 
stable and efficient (but can't handle constant expressions evaluated at compile-time).

- an **_"enriching wrapper"_** that automatically converts every float/double data 
into richer number types, so that you can experiment, at a very low cost, some
beautiful models such as arbitrary precision numbers, interval computation,
or even the marvelous automatic differentiation. 
See [§3](home#3---cojac-the-enriching-wrapper). This second tool is fun but still experimental (some

[limitations](home#62---issues-with-the-wrapper) such as quite naive models implementation, 
strong slowdown, poorly tested support for Java8 lambdas, problem when user-code 
is "called back" from Java library...).

With COJAC you don't have to modify your source code or even recompile. All 
the work is done at runtime, when your application gets instrumented on-the-fly. 

This software is distributed under the "Apache License, v2.0", without any warranty.


## 1.2 - Launching an application with COJAC

COJAC relies on the Java Agent mechanism; our tool is activated via the mere 
insertion of this option in the JVM command line: 
`-javaagent:cojac.jar[=cojac_options]`. 

This mechanism lets you seamlessly activate COJAC at runtime. 
For instance:

```
java -javaagent:cojac.jar -jar MyApplication.jar
java -javaagent:cojac.jar="-Ca -Xf -Xs" -jar MyApplication.jar
java -javaagent:cojac.jar="Rb 100" -cp myFolder my.pkg.MyMain
```

This way it should be widely applicable (applications, servlets, junit test 
case, run from within an IDE or from anywhere else...). 

The integration with existing IDEs should be easy. In Eclipse for instance, 
you could define once, in the context of the "Run Configurations", a new "Variable" 
named `COJAC` with the value `-javaagent:/path/to/cojac.jar="-Xf -Xs"`, 
then you just have to insert `{COJAC}` in the VM arguments to enable the tool 
with your favourite options in any run configurations.

## 1.3 - Links and notes
COJAC is offered (without warranty of any kind) in the hope it can be useful 
for educational purposes, as well as in the software industry. 

You might be interested in the 
[valgrind-based cousin](https://github.com/Cojac/cojac-grind), a 
similar tool not limited to the Java world (it acts on any Linux binary).

The concept of _numerical problem sniffer_ is presented in our 
[article](http://drdobbs.com/testing/232601564) published by Dr Dobb's 
(remember how cool this journal was?). The revolutionary (but still experimental) 
_wrapping feature_ is brand new and has not yet been discussed in any paper.

Any comment/feedback is welcome!

--------------------------------------------------
# 2 - COJAC: the numerical sniffer

The root idea behind COJAC was that it is definitely a pity that Java is not 
able to signal integer overflows at runtime. Then we generalized the idea towards
a full *numerical sniffer*, as discussed in this [article](http://drdobbs.com/testing/232601564).

COJAC is highly configurable, you can specify which operations are to be 
watched, where it should avoid inspecting, and how you want COJAC to warn you about 
the problems. 


## 2.1 - What COJAC considers suspicious

The COJAC sniffer tracks the following kinds of event:

 * *Integer overflow*: the result is out-of-bounds for a `long` or `int` operation (an arithmetic operation, not the bit-shift operations). Examples: `3*Integer.MAX_VALUE, Integer.MIN_VALUE/-1`
 * *Offending typecasting*: a value loses its essence after a type conversion. Examples: `(short) Long.MAX_VALUE, (int) Float.NaN`. We also detect loss
 of precision when converting int-to-float or long-to-double (since release v1.4.1). Example: `(float)Integer.MAX_VALUE`
 * *Smearing*:  adding/subtracting a non-zero floating point number (float or double) has no effect because the two operands have excessively different orders of magnitude. Examples: `(342.0 + 1.0E-43), (342.0 - 1.0E+43)`
 * *Cancellation*: two floating point numbers almost cancel each other after an addition or subtraction, so that the least significant bits (often noise) are promoted to highest significance. Example:  `(3.000001f - 3.0f)`
 * *Questionable comparisons*: two floating point numbers are being compared, but they are very close together. Example: `if (3.000001f >= 3.0f)...`
 * *Underflow*: the result of a division is so small that it gets rounded to zero. Example: `(2.5E-233 / 7.2E+233)`
 * *NaN or Infinite results*: an operations leads to NaN or Infinity, from finite non-NaN operands. Example: `Math.acos(1.00001)`
 
Note that all these example expressions, when written verbatim in Java, 
are evaluated at compile-time because they involve only literal values; so in 
that (uninteresting) case, the problems won't be detected by COJAC (see 
[known issues](home#61---issues-with-the-sniffer)).


--------------------------------------------------
## 2.2 - Configuring what COJAC will detect

COJAC offers a fine-grained configuration of what must be instrumented in the application; the options let you select which category (or even which bytecode instructions) to watch:

 * `-Ca` to activates all possible detectors (this is the default behaviour)
 * `-Cints` to watch the "int" operations: `IADD`,`IDIV`,`IINC`,`ISUB`,`IMUL`,`INEG`
 * `-Clongs` to watch the "long" operations: `LADD`,`LDIV`,`LSUB`,`LMUL`,`LNEG`
 * `-Cdoubles` to watch the "double" operations: `DADD`,`DDIV`,`DSUB`,`DMUL`,`DREM`
 * `-Cfloats` to watch the "float" operations: `FADD`,`FDIV`,`FSUB`,`FMUL`,`FREM`
 * `-Ccasts`  to watch the typecasting operations: `L2I`,`I2S`,`I2C`,`I2B`,`D2F`,`D2I`,`D2L`,`F2I`,`F2L`
 * `-Cmath`  to watch the invocations of java.lang.Math methods, like `Math.sqrt` etc.

By default, all detectors are activated.

COJAC instruments the code everywhere, except in classes from the standard library 
(`java.*`, `javax.*`, ...), and possibly what has been requested to be skipped 
with one of the mechanisms described below.

### 2.2.1 - Excluding some code with -b option

One way of preventing the instrumentation of a class is to use the `-Xb` 
bypass option : 
```
java -javaagent:cojac.jar="-Xb ch.eif.;com.foo.Bar"
```

This will discard the instrumentation of any class whose fully qualified name
starts with "ch.eif." or "com.foo.Bar". 

### 2.2.2 - Excluding some code with annotations

It is possible to exclude particular classes or methods with an annotation
in the source code. The only requirement is to add the following interface
somewhere in your code (in whatever package, but the interface identifier 
has to be strictly respected):

```java
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
@Retention(RUNTIME) public @interface NoCojacInstrumentation {}
```

The annotation is to be placed just before the class or method definition; 
its scope is local.

--------------------------------------------------
## 2.3 - Configuring how COJAC will signal the detected problems

You can  choose the way COJAC will inform you when a problem occurs: 

 * `-Sc`   This is the default behaviour, all the messages are printed to the 
 standard error stream

 * `-Sl file`  All warning messages are appended to a file, the 'file' 
 argument is optional, by default the file is named `COJAC_Report.log`

 * `-Se`  Each problem will throw an `ArithmeticException`

 * `-Sk myMethod`   Each problem will invoke a method of your choice. The method must have that signature: `public static void myMethod(String opcodeName)` and you have to provide its complete qualified path, in the format `pkgA/myPkg/myClass/myMethod`.

You can also configure what information COJAC will give you. Use the `-Sd` 
option to have the detailed stack trace for each warning. You can also use 
the `-Xt` to display the statistics about the instrumentation, namely some 
information about how many bytecode instructions have been instrumented by COJAC. 

COJAC provides a useful `-Xf` option which filters the warnings. For 
instance, if you have the same line that causes the same error many times, 
it's convenient to see only the first occurrence, which is the role of this 
option. If you have enabled the `-Xs` option, all the filtered events will 
be finally synthesized (with occurrences count).  


--------------------------------------------------
## 2.4 - Instrumenting without executing

Early versions of COJAC were able to create "offline" an instrumented version of the 
application code (that can later be executed). We no longer provide this feature: 
COJAC is designed to apply on-the-fly instrumentation, we advocate its use
as a diagnostic-enabled java launching means. 

--------------------------------------------------
## 2.5 - Example

Here is a small example of what COJAC can do. Let's take a simple class like that: 

```java
/*01*/ public class Demo {
/*02*/   public static void main(String[] args) {
/*03*/    int a = 2_000_000_000, b = 2_000_000_000;
/*04*/    int res1 = a + b;
/*05*/    System.out.println("Adding billions: " + res1);
/*06*/    double c = 1E22, d = 321;
/*07*/    double res2 = c + d;
/*08*/    System.out.println("Adding units to a large number : " + res2);
/*09*/    double sqrt = Math.sqrt(-1);
/*10*/    System.out.println("Square root of -1: " + sqrt);
/*11*/  }
/*12*/}
```

Compile it with something like `javac Demo.java`. Then, use COJAC to launch 
this application: `java -javaagent:cojac.jar Demo`.
And you'll get the following output: 

```
COJAC: Overflow : IADD Demo.main(Demo.java:4)
Adding billions: -294967296
COJAC: Smearing: DADD Demo.main(Demo.java:7)
Adding units to a large number : 1.0E22
Square root of -1: NaN
COJAC: Result is NaN: java/lang/Math.sqrt(D)D Demo.main(Demo.java:9)
```

You can see the warning messages in the console. All the COJAC logs start 
with ''COJAC: '', followed by the kind of anomaly and the location in 
the source code (so that in an Eclipse console for instance, you can easily 
get redirected to the edited file).

--------------------------------------------------
## 2.6 - JMX feature

COJAC gives the developer access to detection management through JMX. 
The management interface is the following:

```java
// gives every opcode instrumented with statistics
Map<String, Integer> getCountersMBean(); 
// offers the developer a means to know which methods are subject to annotation
List<String> getBlacklist(); 
// gives every detected event that occurred since the beginning of the application
Map<String, Long> getEvent();
// let you dynamically enable or disable the processing of the detected events.
void start(); 
void stop(); 
```

The RMI service URL is parameterized by a couple of COJAC options. Here is 
the pattern and an example of default configuration:

```
pattern:
service:jmx:rmi:///jndi/rmi://<host>:<port>/<MBean-name>

default: 
service:jmx:rmi:///jndi/rmi://localhost:5017/COJAC
```

You can access the management interface programmatically or using a tool such 
as jconsole. This can be great to watch long-term running applications or 
web services.

--------------------------------------------------
# 3 - COJAC: the enriching wrapper

We have been working on the automatic replacement of the primitive types 
float/double (as well as their "standard wrapper" counterpart Float/Double) by 
objects that realize richer number models.

Now our poor old floating point numbers can be enriched in various ways, 
that we only start to study. There are still limitations, but... yes we did it!

## 3.1 - Our wrapping mechanism

Our wrapping mechanism... is automatic! Just tell us which number model you want to 
activate, and there you go. Everywhere there are float/Float/double/Double data 
in user code (local variables, parameters, attributes...), they will be replaced
by an object redefining the arithmetic behavior. As the code inside the Java library 
cannot be transformed in such a way, we designed a mechanism that automatically 
deals with the necessary conversions when the user code invokes a method of the
Java library (with the sad consequence that the "enrichment" is then lost).

The "arithmetic behavior" that is embedded in our richer number types includes: 

* the basic operations (addition, multiplication...) corresponding to the
 bytecode instructions `dadd`, `dsub`, `dmul`, `ddiv`, `drem`, `dneg`

* the most common operations defined in `java.lang.Math`: `sqrt`, `abs`, 
`sin`, `cos`, `tan`, `asin`, `acos`, `atan`, `sinh`, `cosh`, `tanh`, 
`exp`, `log`, `log10`, `toRadians`, `toDegrees`.

COJAC features a mechanism of *Magic Methods* that can be used to interact with 
the new enriched numbers. A magic method has an identifier starting with 
the prefix `COJAC_MAGIC_` and a specific signature, to be declared in user 
code; at runtime, COJAC will redirect the call to such methods 
towards the implementation inside the wrapper,
thus giving the user code access to the "richer number" power.

The magic methods are model-specific (see below), but every number wrapper will at least 
provide those two methods: 
```java
public static String COJAC_MAGIC_wrapperName();
public static String COJAC_MAGIC_toString(double n); // internal representation
  
```

## 3.2 - Number model "BigDecimal"

Our first "enriched number" type provides arbitrary precision numbers, where
the number of significant digits is specified at runtime. The implementation 
relies on `java.math.BigDecimal`. 

This wrapper is activated with the option `-Rb nDigits`, and it does not 
provide additional magic methods.

Here is a small demo program. 

```java
public class HelloMrCojac {  
  public static String COJAC_MAGIC_toString(double n) { return ""; }
  public static void main(String[] args) {
    double p1=approxPi(10);
    double p2=approxPi(1000);
    double p3=approxPi(1000000);
    System.out.println(p1+" "+COJAC_MAGIC_toString(p1));
    System.out.println(p2+" "+COJAC_MAGIC_toString(p2));
    System.out.println(p3+" "+COJAC_MAGIC_toString(p3));
  }
  public static double approxPi(long m) {
    double r = 1.0, dm, dp, k;
    for (long i=2; i<=m; i=i+2) {
      k = 2*i;
      dm = 1.0/(k-1);
      dp = 1.0/(k+1);
      r  = r - dm + dp;
    }
    return 4*r;
  }
}
```

A normal run will produce that output: 

```
prompt> java HelloMrCojac
3.232315809405594
3.1425916543395442
3.1415936535887745
```

Now, let's compute with twice as much significant digits:

```
prompt> java -javaagent:cojac.jar="-Rb 30" HelloMrCojac
3.2323158094055926 3.23231580940559268732643345647
3.142591654339543  3.14259165433954305090112773730
3.141593653588793  3.14159365358879323921264313231
```


Just for fun, let's show that we can also decrease the internal precision 
down to only 2 significant digits:

```
prompt> java -javaagent:cojac.jar="-Rb 2" HelloMrCojac
3.3 3.3
3.3 3.3
3.3 3.3
```

## 3.3 - Number model "Interval computation"

The traditional float/double types compute approximate results (due to possible 
rounding), but they can give no information at all about how far we get from 
the "true" mathematical result. The idea behind the *interval computation* technique of *reliable computing*  is to represent numbers with an interval that is guaranteed to hold the true result, thus keeping track of the possible error margin. 

Our Interval wrapper is a toy realization of this technique. Internally, we emit
a warning whenever the relative error of a number goes beyond a certain threshold 
(that can be adjusted via the `-R_unstableAt` option (default value is 0.00001)).

This wrapper is activated with the option `-Ri`. Two additional magic methods are defined for this model:
```java
public static double COJAC_MAGIC_relativeError(double n);
public static double COJAC_MAGIC_width(double n); // interval width max-min
```

For now, we are yet far from the quality of a good "interval computation" library. For 
instance, we systematically round up/down instead of adjusting the 
rounding mode. Nevertheless, the strength and simplicity of the automatic wrapping 
should not be underestimated.

## 3.4 - Number model "Discrete stochastic arithmetic"

Interval computation is known to offer strong guarantees, but also to be overly 
pessimistic in the estimation of the relative error (the interval is often far 
wider than needed). _Discrete stochastic arithmetic_ is an interesting 
alternative: roughly, the idea is to keep 3 versions of every arithmetic
result, each corresponding to the application of a rounding mode chosen _randomly_. 
An unstable computation has a high probability of producing 3 manifestly divergent 
results, and the relative error can be estimated.

For the moment, we don't follow the full-strength theory behind stochastic
arithmetic, and simply round up/down randomly. We claim again that the 
real breakthrough lies in the ease of our tool, which helps experimenting
with the model.

This wrapper is activated with the option `-Rs`. 
That wrapper offers the same magic methods as our interval computation model, 
and is similar in its way of signaling high relative errors.

## 3.5 - Number model "Automatic differentiation"

*Automatic differentiation* is an awesome idea (never heard of it? You _should_ have 
a look!). One reason the technique is not in widespread use lies in the fact that 
there is a lack of *simple* tools that can show its power. We pretend that the 
situation dramatically changes thanks to COJAC. 

This wrapper is activated with the option `-Ra`. 
Here are the magic methods that are provided:
```java
public static double COJAC_MAGIC_getDerivation(double a);
public static double COJAC_MAGIC_asDerivationTarget(double a);
```

Let's show them in action with a small example.

```java
public class DerivationDemo {
  public static double COJAC_MAGIC_getDerivation(double a)      { return 0; }
  public static double COJAC_MAGIC_asDerivationTarget(double a) { return a; }
  static double someFunction(double x, double a, double b) {
    double res=a*x*x;     // the computation can be complex,
    res = res + b*x;      // with loops, calls, recursion etc.
    res = res + 1;
    return res;     // f: a X^2 + b X + 1        f': 2aX + b
  }
  public static void main(String[] args) {
    double x=2;
    x=COJAC_MAGIC_asDerivationTarget(x);  // we'll compute df/dx
    double y=someFunction(x, 3, 4);
    System.out.println("f(2):  "+y);
    System.out.println("f'(2): "+COJAC_MAGIC_getDerivation(y));
  }
```

And we run it with the appropriate Derivation wrapper:

```
prompt> java -javaagent:cojac.jar="-Ra" DerivationDemo
f(x):  21.0
f'(x): 16.0
```
## 3.6 - Number model "Symbolic computation"

Symbolic computation is known for representing numbers and mathematical functions in an exact finite way. On the other hand the symbolic computation provides functionalities such as compute the derivative of a function, or finding its roots.

We attempted a similar approach by storing the whole symbolic expression tree  of a number. What makes that numbers become functions.

Through this mechanism, we are now able to store, evaluate and differentiate functions.

This wrapper can be activated via the "-Rsy" option. Here are the magic methods available:

```java
public static double COJAC_MAGIC_asSymbolicUnknown(double a);
public static double COJAC_MAGIC_evaluateSymbolicAt(double d, double x);
public static double COJAC_MAGIC_derivateSymbolic(double d);
```
And here is a small example of what we can do with this wrapper.

```java
public class ATinySymbolicDemo {
  public static String COJAC_MAGIC_toString(double n) {return "";}
  public static double COJAC_MAGIC_asSymbolicUnknown(double a) {return a;}
  public static double COJAC_MAGIC_evaluateSymbolicAt(double d, double x) {return d;}
  public static double COJAC_MAGIC_derivateSymbolic(double d) {return d;}
  // f(x) = 3x^2 + 2x + 5
  static double myFunction(double x) {
    double res = 3 * x * x; 
    res = res + 2 * x; 
    res = res + 5;
    return res; 
  }
  public static void main(String[] args) {
    double x = COJAC_MAGIC_asSymbolicUnknown(0.0); // define the unknown
    double f = myFunction(x);                      // define the function
    double df = COJAC_MAGIC_derivateSymbolic(f);   // compute the derivative
    System.out.println("f(x)  = " + COJAC_MAGIC_toString(f));  // print the function "f"
    System.out.println("f'(x) = " + COJAC_MAGIC_toString(df)); // print the derivative of "f"
    System.out.println("f(2)  = " + COJAC_MAGIC_evaluateSymbolicAt(f, 2)); // compute the results of the function
    System.out.println("f'(2) = " + COJAC_MAGIC_evaluateSymbolicAt(df, 2));// compute the results of the derivative
  }
}
```
And that's how we run this program with the Symbolic wrapper and the result:

```
prompt> java -javaagent:cojac.jar="-Rsy" ATinySymbolicDemo
f(x)  = ADD(ADD(MUL(MUL(3.0,x),x),MUL(2.0,x)),5.0)
f'(x) = ADD(ADD(ADD(MUL(ADD(MUL(0.0,x),MUL(3.0,1.0)),x),MUL(MUL(3.0,x),1.0)),ADD(MUL(0.0,x),MUL(2.0,1.0))),0.0)
f(2)  = 21.0
f'(2) = 14.0
```
We can see that the results are those expected.

Another feature is to exploit the symbolic representation to improve the accuracy of the calculated amounts. And that by using the Kahan summation algorithm.

Here is the corresponding magic method:
```java
public static double COJAC_MAGIC_evaluateBetterSymbolicAt(double d, double x);
```
And here is an example:

```java
public class ATinySymbolicDemo {
  public static double COJAC_MAGIC_evaluateSymbolicAt(double d, double x) {return d;}
  public static double COJAC_MAGIC_evaluateBetterSymbolicAt(double d, double x) {return d;}
  public static void main(String[] args) {
    double[] t = {+2, 1E-16, 1E-16, 1E-16, 5E-17, 5E-17};
    double sum = 0;
    for(double e : t) sum + = e;
    System.out.println("sum  = " + COJAC_MAGIC_evaluateSymbolicAt(sum, 0.0));      // compute the sum (standard)
    System.out.println("sum  = " + COJAC_MAGIC_evaluateBetterSymbolicAt(sum, 0.0));// compute the sum (better)
  }
}
```

```
prompt> java -javaagent:cojac.jar="-Rsy" ATinySymbolicDemo
sum  = 2.0
sum  = 2.0000000000000004
```
By observing the results, we can see that the accuracy of the summation is improved.


## 3.7 - Number model "Chebfun"

Chebfun is an open-source library for MATLAB that uses polynomial approximations for representing functions. This polynomial representation allows operations such as compute finite integral or derivative.

We are based on this concept to create a new wrapper. This wrapper allow to define functions (those will be polynomial approximations). For now we are able to define, evaluate and differentiate functions.

This wrapper can be activated via the "-Rcheb" option. Here are the magic methods available:

```java
public static double COJAC_MAGIC_asChebfun(double a) {return a;}
public static double COJAC_MAGIC_evaluateChebfunAt(double d, double x) {return d;}
public static double COJAC_MAGIC_derivateChebfun(double d) {return d;}
```
And here is a small example of what we can do with this wrapper.

```java
public class ATinyChebfunDemo {
  public static String COJAC_MAGIC_toString(double n) {return "";}
  public static double COJAC_MAGIC_asChebfun(double a) {return a;}
  public static double COJAC_MAGIC_evaluateChebfunAt(double d, double x) {return d;}
  public static double COJAC_MAGIC_derivateChebfun(double d) {return d;}
  public static void main(String[] args) {      
    double x = COJAC_MAGIC_asChebfun(0.0); // define a function x=x
    double f = Math.sin(2*x); // apply operations on x
    double df= COJAC_MAGIC_derivateChebfun(f); // compute the derivative of f
    System.out.printf("f(x) = %s should be (%s) \n", COJAC_MAGIC_evaluateChebfunAt(f, 0.5123), Math.sin(2*0.5123));
    System.out.printf("f'(x) = %s should be (%s) \n", COJAC_MAGIC_evaluateChebfunAt(df, 0.5123),2*Math.cos(2*0.5123));   
  }
}

```
And that's how we run this program with the Chebfun wrapper and the result:

```
prompt> java -javaagent:cojac.jar="-Rcheb" ATinyChebfunDemo
f(x) = 0.854506481547763 should be (0.8545064815477629) 
f'(x) = 1.0388814619442395 should be (1.0388814619442641) 
```
We can see that the results are those expected (at 13 digits).

As you can check, the results are those expected!
--------------------------------------------------
# 4 - Changing Java's primitive type behaviour

Cojac implements multiple new behaviours for Java, one of which being the numerical sniffer. We have tried to add up to Java behaviours that we felt were missing, like in the sniffer, where we fill Java's holes about IEEE754 floating-point implementation, that was lacking *overflow*, *smearing*, and so on. Now, we also added some behaviours to Java for doing much more other things:

* Double as float casting, to try out a program's reaction to less precision calculation (to see if the program's behaviour drastically changed or if the result stays reasonable).

* Change (downwards) arbitrarily the precision of all floating-point calculation, with the same goal as the transformation of double as float, but with more freedom.

* Change the rounding mode of java's arithmetic
  * *Artificially*, by adding or removing ulps.
  * Natively, with C code that allows to change the processor's rounding mode.

* Use all double as intervals (with bounds of roughly float precision)

## 4.1 - Double as float casting

This mode downgrades every double as a float, therefore testing the arithmetic stability of a program. If your program doesn't produce a similar, less precise result with this mode, you should check (maybe with the other numerous possibilities offered by Cojac?)

This behaviour is activated with the option `-BD2F`

Here is a visual example of what it changes to Mandelbrot's fractal ( Java SE Development Kit demo, that you can find here: http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) 

Here is a detail of the fractal, with original (double) precision:

<p align="center">
<img src="/images/MandelbrotDetail.PNG"  width=600"/>
</p>
And here the same detail when instrumented with `-BD2F`

<p align="center">
<img src="/images/MandelbrotDetail(-BD2F).PNG"  width=600" align="center"/>
</p>
The demo works perfectly fine with less precision, the result is similar with less details, but there is no errors.

## 4.2 - Arbitrarily low floating-point arithmetics

Similarly to the conversion to floats, this behaviour allows to test the effect of having less precise floating point operation (Want to see what results an 8-bit computer would give?)

The option for this option is `-Ap <bits>`, where `<bits>` is the numbers of bits that will be used in the mantissa. If the precision is set to be less than 23 (float's mantissa size) it will affect floats as well as double.

Here is now an example with a mantissa of 5.

First, a view of the Mandelbrot fractal with double precision.

<p align="center">
<img src="/images/MandelbrotFull.PNG"  width=600" align="center"/>
</p>
And now the same view, with Cojac and the option `-Ap 5`.

<p align="center">
<img src="/images/MandelbrotFull(-Ap5).PNG"  width=600" align="center"/>
</p>
The programm is stable, even with an ultra-low precision, and the result is very *pixelated*, yet similar to the original.

## 4.3 - Rounding mode in floating-point arithmetic
In Java, we don't have access to the CPU's rounding modes. This is a serious deficiency in Java's IEEE 754 floating point *support*, which just uses the nearest possible float/double. The use of rounding modes are multiple, even if it won't affect every programmer.
* It can be used as a way of testing the stability of mathematical functions or programs, that souldn't give much different results under a different rounding mode
* It can bring certainty in a result. For example, in an interval, rounding the value of the inferior bound down is more pessimistic than rounding to the nearest, but then the *real* value (with infinite precision) represented by the interval is assured to be included. The same goes with the upper bound, that should be rounded up.

We want to (partially) resolve that with cojac, in two different manners that both present pros and cons.

### 4.3.1 - Artificial *rounding* mode
The first solution (that never should called a solution) we invented is to add and remove ulps after every computation.
This has been done because, as Java doesn't provide natively access to the CPU's rounding modes, it was a simple, with relatively low performance impact and native way of ensuring that the result value goes in the selected direction.

The benefit is that we provide a native (meaning it can be run on every platform supporting Java) pseudo rounding mode.

The drawback is that every operation, even if no rounding is needed, will be affected, as we will see in this small example.

0.25 + 0.25, under normal conditions, give 0.5, with no rounding needed, as the three number are exactly represented with IEEE 754 floats.
With artificial rounding up, the result will be 0.5 + ulp(0.5), which is unnecessary imprecise.

The modes provided by this behaviour are: 
* "Round" up (toward infinity), with the option `-Rbu`
* "Round" randomly up or down with the option `-Rbr`
* "Round" down (toward -infinity) with the option `-Rbd`

This behaviour can then be useful, but keep its limitations in mind when using it. This won't replace a CPU's modes.

### 4.3.2 - Native rounding mode
The second solution we implemented is to use JNI (Java Native Interface) to change the processor's mode, with a C routine.

The drawback of that is that we lose Java's portability. The C method has to be compiled in various libraries, one for each platform, those libraries have to be included in cojac, and the Java code may have to be modified for including the correct version. This also means that tests should be run on every platform on which we may want to run cojac. All of which is very time consuming and impractical

The benefit is that the CPU's rounding modes (the true, unbiased rounding modes) become finally accessible to Java programs.

The modes provided by this behaviour are: 
* Round up, with the option `-Rnu`
* Round toward zero with the option `-Rbz`
* Round down with the option `-Rbd`

Notes: 
* If the native library fails to be loaded, unit tests are skipped, and if using one of the above options, Cojac stops and throws a RuntimeException.
* Libraries have been created for Windows (32 & 64 bits), and Linux (64 bits) and only the 64 bits versions have been tested.
* The source C code is included in Cojac/src/main/resources/native-libraries/src, feel free to compile them for your system (and include the added library in  com.github.cojac.models.behaviours.ConversionBehaviour)
 
## 4.4 - Double as float interval
As replacing with objects poses some problems, like signature incompatibility, problems with arrays, we tried to give a similar tool to the interval wrapper, without changing the content of the stack (i.e. without replacing doubles). What we did is to use double as two (near-float) values, storing the (pessimistic) bounds of the *real* value. This allows to react (with prints, exceptions or callback) when the relative error gets bigger than a given threshold (default value: 1E-5, user definable with `-R_unstableAt <newValue>`)

Drawback: due to the representation used, the interval value quickly loses precision, even for operations for which it shouldn't happen (interval wrapper does not have the same problem, for instance)

## 4.5 - Create a new behaviour
For adding those behaviours easily, a powerful mechanism has been implemented, allowing to create a new behaviour in minutes, when following some convention.
It is possible to change the following things:
* ByteCode operations (Exclusively: IADD, ISUB, IMUL, IDIV, INEG, IINC, ICONST_M1, ICONST_0, ICONST_1, ICONST_2, ICONST_3, ICONST_4, ICONST_5, LADD, LSUB, LMUL, LDIV, LNEG, LCONST_0, LCONST_1, FADD, FSUB, FMUL, FDIV, FREM, FCMPL, FCMPG, FNEG, FCONST_0, FCONST_1, FCONST_2, DADD, DSUB, DMUL, DDIV, DREM, DCMPL, DCMPG, DNEG, DCONST_0, DCONST_1, I2S, I2C, I2B, I2F, I2L, I2D, L2I, L2F, L2D, F2I, F2D, F2L, D2F, D2I, D2L)
* java.lang.Math public static methods
* Any public static methods

### 4.5.1 - Replacing a ByteCode operation or a Math (public static) method
In the behaviour's class, write a method corresponding to the operation's *signature*  (you will find those here: https://docs.oracle.com/javase/specs/jvms/se8/html/jvms-6.html), or to the java.lang.Math's method signature (https://docs.oracle.com/javase/8/docs/api/java/lang/Math.html).

Example: DADD takes two double from the stack and returns one, its signature has to be `public static double DADD(double, double)`. 

```
//this will be called in place of a double addition in the instrumented code.
public static double DADD(double a, double b) {
       return a + Math.PI - b -Math.PI ;
}
```
For replacing the Math method "ceil" (signature: `public static double ceil(double a)`):
```
//this will be called in place of a call to Math.ceil in the instrumented code.
public static double ceil(double a) {
        double c = Math.ceil(a); // Math calls can be performed
       return a + c / 2 ;
}
```

### 4.5.3 - Replacing a public static method
For replacing any public static method (from other classes than java.lang.Math), an annotation `FromClass` with the class's fully qualified name as value must be added to the method and the signature of the method respected.

Example: If you want to instrument the method `public static int bar(String)` from the class `com.package.Foo`:
```
@FromClass("com/package/Foo")
public static int bar(String s){
        return Integer.MAX_VALUE;
}
```
### 4.5.4 - Ignoring a method in the behaviour class
Cojac checks if the methods in the behaviour class correspond to an actual method (or operation). For utility, setup, tear down or other methods, an annotation `@UtilityMethod` indicated to Cojac that it shall ignore this method.


## 4.6 - Selective instrumentation
For some behaviours, it could be interesting to instrument only parts of the code. Cojac offers two ways of selecting what to instrument:
* By excluding some classes with the option `-Xb`, like for wrappers
* By selecting all instrumented class with the option `-Oi <Classes>` 

Syntax diagram of `<Classes>`: 
 
![Classes](images/Classes.png "Classes")

Class
![Class](images/Class.png "Class")

MethodsOrLines
![Methods or line number](images/MethodsOrLines.png "Methods or line number")

Operations
![Operations](images/Operations.png "Operations")

Interval
![Interval](images/Interval.png "Interval")

Example:
`-Oi com.package.Foo{method()I,1-25,34,99_22-27,100};com.package.Foobar`
Will fully instrument class Foobar,
and partially instrument class Foo, as following:

* method()I fully
* lines 1 to 25, 34 and 99
* instructions 22 to 27 and 100

--------------------------------------------------
# 5 - Detailed usage

Here is the full manpage-like description of COJAC, as produced by the help argument (`java -javaagent:cojac.jar="-h"`): 

```
usage: java -javaagent:cojac.jar="[OPTIONS]" YourApp [appArgs]
            (version 1.4 - 2015 Oct 21)

Two nice tools to enrich Java arithmetic capabilities, on-the-fly:
- Numerical Problem Sniffer: detects and signals arithmetic poisons like integer
  overflows, smearing and catastrophic cancellation, NaN or infinite results.
- Enriching Wrapper for float/double: wraps every double/float in richer objects. 
  Current models include BigDecimal (you choose the precision), interval computation,
  discrete stochastic arithmetic, and even automatic differentiation.
```
```
----------------- OPTIONS -----------------
 -Ca,--all                   Sniff everywhere (this is the default behavior)
 -Ccasts                     Sniff in casts opcodes
 -Cdoubles                   Sniff in doubles opcodes
 -Cfloats                    Sniff in floats opcodes
 -Cints                      Sniff in ints opcodes
 -Clongs                     Sniff in longs opcodes
 -Cmath                      Sniff in (Strict)Math.xyz() methods
 -Cn,--none                  Don't sniff at all
 -Copcodes <arg>             Sniff in those (comma separated) opcodes; eg:
                             iadd,idiv,iinc,isub,imul,ineg,ladd,lsub,lmul,ldiv,l
                             neg,dadd,dsub,dmul,ddiv,drem,dcmp,fadd,fsub,fmul,fd
                             iv,frem,fcmp,l2i,i2s,i2c,i2b,d2f,d2i,d2l,f2i,f2l
 -h,--help                   Print the help of the program and exit
 -jmxenable                  Enable JMX feature
 -jmxhost <host>             Set remote JMX connection host (default: localhost)
 -jmxname <MBean-id>         Set remote MBean name (default: COJAC)
 -jmxport <port>             Set remote JMX connection port (default: 5017)
 -R_noUnstableComparisons    Disable unstability checks in comparisons, for the
                             Interval or Stochastic wrappers
 -R_unstableAt <epsilon>     Relative precision considered unstable, for
                             Interval/Stochastic wrappers (default 0.00001)
 -Ra,--autodiff              Use automatic differentiation wrapping
 -Rb,--bigdecimal <digits>   Use BigDecimal wrapping with a certain precision
                             (number of digits).
                             Example: -Rb 100 will wrap with
                             100-significant-digit BigDecimals
 -Ri,--interval              Use interval computation wrapping
 -Rs,--stochastic            Use discrete stochastic arithmetic wrapping
 -Rsy,--symbolic             Use symbolic computation wrapping
 -Rcheb,--chebfun            Use chebfun wrapping
 -Sc,--console               Signal problems with console messages to stderr
                             (default signaling policy)
 -Sd,--detailed              Log the full stack trace (combined with -Cc or -Cl)
 -Se,--exception             Signal problems by throwing an ArithmeticException
 -Sk,--callback <meth>       Signal problems by calling a user-supplied method
                             matching this signature:
                             ...public static void f(String msg)
                             (Give a fully qualified identifier in the form:
                             pkgA/myPkg/myClass/myMethod)
 -Sl,--logfile <path>        Signal problems by writing to a log file.
                             Default filename is: COJAC_Report.log.
 -v,--verbose                Display some internal traces
 -Xb,--bypass <prefixes>     Bypass classes starting with one of these prefixes
                             (semi-colon separated list).
                             Example: -Xb foo;bar.util
                             will skip classes with name foo* or bar.util*
 -Xf,--filter                Report each problem only once per faulty line
 -Xs,--summary               Print runtime statistics
 -Xt,--stats                 Print instrumentation statistics

------> https://github.com/Cojac/Cojac <------ 
```

--------------------------------------------------

# 6 - Limitations and known issues

This section discusses a couple of issues for the current version of Cojac.

## 6.1 - Issues with the sniffer

The sniffer part of COJAC is rather stable. Here are some limitations:

 * Of course a suspicious operation is not always the manifestation of a software defect. For instance, you can rely on integer overflows to compute a hash function, or maybe the cancellation phenomenon is not a problem because the floating point numbers you deal with do not suffer from imprecision.

 * That's inherent to the approach: compile-time expressions can't be processed: 
 don't complain that a "hello world" use case like
`System.out.println(3*Integer.MAX_VALUE);` does not log the overflow!
 
 * The tool targets the JVM only. We focus on Java, but it might be interesting 
 to try it on other JVM-equipped languages (Scala, Jython...). By the way, if 
 you dream of a numerical sniffer not limited to the Java world, have a look at 
 [cojac-grind](https://github.com/Cojac/cojac-grind)...


## 6.2 - Issues with the wrapper

The wrapper part of COJAC should be considered an experimental prototype. Here 
are some limitations:

* The frontier between "user code" and "java library" has some serious defects, eg 
when arrays of numbers are involved.

* We don't handle the "callbacks" from java library to user code when floating 
point numbers are being passed around.

* We have tried to handle `invokedynamic` (at least how Java8 compilers 
use it), but it has not been thoroughly  tested yet, so we expect some problems 
with Java8 lambdas (Cojac 1.4.1 has fixed some problems).

* In case of `class A extends J` where `J` is from the java library, and
offers a method `f()` (with floating point parameters/result) that `A` does
not redefine : suppose the declaration `A a`, then the call `a.f(...)` fails,
whereas `((J)a).f(...)` is OK. This should be fixed in Cojac 1.4.1.

* We don't handle the possible use of Java reflection (in case of method 
invocations via reflection, we don't apply the necessary transformations). 

* The decision of converting both the primitive types float/double and their
original wrapper Float/Double brings several problems, eg signature conflicts, or 
comparison (compareTo/equals) misbehavior.

* The implementation of the models is really naive. For instance, we do not compute
every Math.* operations with the required precision in the BigDecimal model.

* Of course, the slow-down is very important.

--------------------------------------------------

# 7 - And now...

...well, happy problem sniffing, and happy number wrapping!  

<tt>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</tt>  ...please give us feedback :smiley: via an issue on github, or `the.google.tool@gmail.com`.

<tt>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</tt>  The COJAC Team. 

--------------------------------------------------
### Acknowledgements 

Several Computer Science students from [HEIA-FR](https://www.heia-fr.ch) 
have been involved in some way in the COJAC project: 

- Ruggiero Botteon and Diego Cavadini (for the very first pre-prototype)
- Baptiste Wicht (the major contributor, strong refactoring, testing - many thanks!)
- Maxime Reymond (for an Eclipse Plugin, now (temporarily?) discontinued)
- Vincent Pasquier (for switching to the "Java agent" technology)
- Luis Domingues (for improving the Valgrind cousin Cojac-grind)
- Romain Monnard (without whom the "wrapping" would still be an unimplemented dream - many thanks!)
- Sylvain Julmy (for populating 3 "rich number" types)
- Lucy Linder (for improving the whole, preparing the release on GitHub, and much more - many thanks!)
