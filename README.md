# CHAT.IXI

<img src="https://cdn-images-1.medium.com/max/2000/1*keoOf8EkZLrue7eLAxjCig.png" />

## About

**CHAT.ixi** is an [IXI (IOTA eXtension Interface) module](https://github.com/iotaledger/ixi) for the [Iota Controlled agenT (Ict)](https://github.com/iotaledger/ict).
It extends the functionality of the core Ict client with a chat application which allows users in the Ict network
to exchange messages on a permissionless, distributed data-integrity protocol - the Tangle.

CHAT.ixi is described in more detail in this official [IOTA blog post](https://blog.iota.org/chat-ixi-using-ict-for-permissionless-chat-on-the-iota-tangle-59ce6c5b95fb).

## Installation

### Step 1: Install and Run Ict

Please find instructions on [iotaledger/ict](https://github.com/iotaledger/ict#installation). There are a few
community made resources for the Ict installation. Use them at your own risk:
* [IOTA Omega-Ict tutorial: noob edition](https://medium.com/@lambtho/iota-omega-ict-tutorial-noob-edition-ff9e1e6d6c2f) (Guide) by Lambtho
* [ict-install](https://github.com/phschaeff/ict-install) (Script) by phschaeff

Make sure you are connected to the main network and not to an island, otherwise you won't be able to message anyone in the main network.

### Step 2: Install CHAT.ixi

There are two ways to do this:

#### Simple Method

Go to [releases](https://github.com/iotaledger/chat.ixi/releases) and download the **chat.ixi-{VERSION}.zip**
from the most recent release. Unzip its content into any directory.

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
gradle fatJar
```

### Step 3: Run CHAT.ixi

```shell
# Please replace {ICT} with the name of your Ict. You can find it in your ict.cfg file. The default setting is 'ict'.
# Set 'ixi_enabled=true' in your ict.cfg configuration file.
# Also replace {USERNAME} with the username you want to appear with in the chat.
# And {PASSWORD} with any password to protect your CHAT.ixi from unauthorized access.
java -jar chat.ixi-{VERSION}.jar {ICT} {USERNAME} {PASSWORD}
# EXAMPLE: java -jar chat.ixi-1.2.3.jar my_cool_ict SatoshiNakamoto
```

### Step 4: Open the Web GUI

Open web/index.html in your web browser. If you are running Ict locally, it should immediately connect you. If no CHAT.ixi
instance is running on localhost, it will ask you for the ip address of your Ict node.

<img src="https://cdn-images-1.medium.com/max/2000/1*CxDGQSYolCIYtKNA4_4WcA.png" />

## Disclaimer

While having the CHAT.ixi web GUI open, your username and user id will be submitted to the network in regular
time intervals, so that other users can see that you are online.

We are not responsible for any damage caused by running this software. Please use it at your own risk.
