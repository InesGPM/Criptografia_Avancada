# UDP Streaming Proxy

Projeto de streaming de vídeo via UDP com um proxy intermédio, desenvolvido em Java.  
O servidor envia o vídeo para o proxy, que depois encaminha os pacotes para o VLC.

O projeto tem três versões:

- DRBG
- CHA
- AES

---

# Como Executar

A execução deve ser feita em três passos:

1. Executar o Proxy
2. Abrir o VLC
3. Executar o Servidor

---

# 1. Compilar e executar o Proxy

Entrar na pasta do proxy:

```bash
cd hjUDPproxy
```

### DRBG

```bash
javac hjUDPproxy.java
java hjUDPproxy
```

### CHA

```bash
javac hjUDProxyCHA.java
java hjUDProxyCHA
```

### AES

```bash
javac hjUDProxyAES.java
java hjUDProxyAES
```

---

# 2. Abrir o VLC

Abrir o VLC para receber o stream na porta **7777**.

```powershell
& "C:\Program Files\VideoLAN\VLC\vlc.exe" udp://@:7777
```

Ou no próprio VLC:

```
Media → Open Network Stream → udp://@:7777
```

---

# 3. Compilar e executar o Servidor

Entrar na pasta do servidor:

```bash
cd hjStreamServer
```

### DRBG

```bash
javac hjStreamServer.java
java hjStreamServer movies\monsters.dat localhost 8888
```

### CHA

```bash
javac hjStreamServerCHA.java
java hjStreamServerCHA movies\monsters.dat localhost 8888
```

### AES

```bash
javac hjStreamServerAES.java
java hjStreamServerAES movies\monsters.dat localhost 8888
```

---

# Fluxo do Sistema

```
Servidor → Proxy → VLC
```

O servidor envia os pacotes UDP para o proxy, que os encaminha para o VLC para reprodução.
