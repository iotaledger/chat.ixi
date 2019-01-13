# CHAT.IXI

<img src="https://cdn-images-1.medium.com/max/2000/1*keoOf8EkZLrue7eLAxjCig.png" />

## About

**CHAT.ixi** is an [IXI (IOTA eXtension Interface) module](https://github.com/iotaledger/ixi) for the [Iota Controlled agenT (Ict)](https://github.com/iotaledger/ict).
It extends the functionality of the core Ict client with a chat application which allows users in the Ict network
to exchange messages on a permissionless, distributed data-integrity protocol - the Tangle.

CHAT.ixi is described in more detail in this official [IOTA blog post](https://blog.iota.org/chat-ixi-using-ict-for-permissionless-chat-on-the-iota-tangle-59ce6c5b95fb).

## Installation

### Step 1: Install Ict

Please find instructions on [iotaledger/ict](https://github.com/iotaledger/ict#installation).

Make sure you are connected to the main network and not to an island, otherwise you won't be able to message anyone in the main network.

### Step 2: Get CHAT.ixi

There are two ways to do this:

#### Simple Method

Go to [releases](https://github.com/iotaledger/chat.ixi/releases) and download the **chat.ixi-{VERSION}.jar**
from the most recent release.

#### Advanced Method

You can also build the .jar file from the source code yourself. You will need **Git** and **Gradle**.

```shell
# download the source code from github to your local machine
git clone https://github.com/iotaledger/chat.ixi
# if you don't have git, you can also do this instead:
#   wget https://github.com/iotaledger/chat.ixi/archive/master.zip
#   unzip master.zip

# change into the just created local copy of the repository
cd chat.ixi

# build the chat.ixi-{VERSION}.jar file
gradle ixi
```

### Step 3: Install CHAT.ixi
Move chat.ixi-{VERSION}.jar to the **modules/** directory of your Ict:
```shell
mv chat.ixi-{VERSION}.jar ict/modules
```

### Step 4: Run Ict
```shell
# switch back to Ict folder where .jar of Ict is located
cd ../ict

# run Ict
java -jar ict-{VERSION}.jar
```

### Step 5: Configure CHAT.ixi (Optional)

CHAT.ixi is automatically configured at the first start.

To configure it manually, open in **ict/modules/chat-config/chat.cfg** and change the parameters accordingly:

Replace **{USERNAME}** with the username you want to appear with in the chat.
Replace **{PASSWORD}** with any password to protect your CHAT.ixi from unauthorized access.

*ict/modules/chat-config/chat.cfg*
```
username={USERNAME}
password={PASSWORD}
```

### Step 6: Open the Web GUI

Open **http://{HOST}:2019** in your web browser, where {HOST} is the IP address at which your Ict is running.

<img src="https://cdn-images-1.medium.com/max/2000/1*CxDGQSYolCIYtKNA4_4WcA.png" />

## Disclaimer

While having the CHAT.ixi web GUI open, your username and user id will be submitted to the network in regular
time intervals, so that other users can see that you are online.

We are not responsible for any damage caused by running this software. Please use it at your own risk.
