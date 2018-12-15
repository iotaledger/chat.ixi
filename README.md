# IOTA Extension Interface (IXI)

## About

The IOTA Extension Interface provides an API that connects IXI modules to the [Ict](https://github.com/iotaledger/ict) core client.
IXI modules are applications built on top of the Tangle protocol.

## Creating an IXI

### Step 1: Clone this Repository

```shell
# clone the example source code
git clone https://github.com/iotaledger/ixi
```

You can also manually download the repository source code if you don't have **Git**.

### Step 2: Implement your IXI Module

This part is where you get creative. Implement your ideas in `src/main/java/org.iota.ixi/Ixi.java`. Make sure to change
the name from `example.ixi` to whatever your IXI module is called.

### Step 3: Build your IXI.jar

This step requires **Gradle**.

```shell
# move into the cloned repository (in which your build.gradle file is)
cd Desktop/ixi

# build the .jar file
gradle fatJar
```

You should now find your finished `ixi-{VERSION}.jar` file.

## Run the IXI on your Ict

### Step 1: Start your IXI.jar

```shell
java -jar ixi-{VERSION}.jar
```

### Step 2: Add the IXI to your ict.cfg

In your **ict.cfg** file, add your IXI to `ixis` and make sure that `ixi_enabled` is set to `true`:

```
ixis=my_cool_app.ixi
ixi_enabled = true
```

### Step 3: Start your Ict Client

```shell
java -jar ict-{VERSION}.jar
```

If everything went successfully, your Ict should print this line:

```
connecting to IXI 'my_cool_app.ixi' ... success
```

Your IXI should now be running.