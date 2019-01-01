# CHAT.IXI

<img src="https://cdn-images-1.medium.com/max/2000/1*keoOf8EkZLrue7eLAxjCig.png" />

## About

**chat.ixi** is an [IXI (IOTA Extension Interface) module](https://github.com/iotaledger/ixi) for the [Iota Controlled Agent (Ict)](https://github.com/iotaledger/ict).
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
git clone https://github.com/iotaledger/chat.ixi
cd chat.ixi
gradle fatJar
```

### Step 3: Run CHAT.ixi

```shell
# move into whatever directory you put your chat.ixi-{VERSION}.jar into
cd Desktop/chat.ixi/

# Please replace {ICT} with the name of your Ict. You can find it in your ict.cfg file. The default setting is 'ict'.
# Also replace {USERNAME} with the username you want to appear with in the chat.
java -jar chat.ixi-{VERSION}.jar {ICT} {USERNAME}
```

### Step 4: Open the Web GUI

Open web/index.html in your web browser. If you are running Ict locally, it should immediately connect you. If no CHAt.ixi
instance is running on localhost, it will ask you for the ip address of your Ict node.

<img src="https://cdn-images-1.medium.com/max/2000/1*CxDGQSYolCIYtKNA4_4WcA.png" />

## Disclaimer

While having the CHAT.ixi web GUI open, your username and user id will be submitted to the network in regular
time intervals, so that other users can see that you are online.

We are not responsible for any damage caused by running this software. Please use it at your own risk.
