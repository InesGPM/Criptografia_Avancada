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

##

# DPRG-based Stream Encryption

Na implementação baseada em **DPRG (Deterministic Pseudo Random Generator)** segui um modelo semelhante a **OTP (One-Time Pad)** para cifrar os frames do stream.

O servidor e o proxy partilham previamente uma **chave secreta**. Para cada frame, o servidor usa essa chave juntamente com um **contador de frames** para gerar uma sequência pseudo-aleatória de bytes (keystream). Esse keystream é depois combinado com os dados do frame através de **XOR**, produzindo o frame cifrado.

O pacote enviado pelo servidor contém:
- o **contador do frame**
- o **frame cifrado**

Quando o proxy recebe o pacote, usa a **mesma chave secreta** e o **contador recebido** para gerar novamente o mesmo keystream através do DPRG. Aplicando **XOR** entre o keystream e o frame cifrado, o proxy recupera o frame original, que depois é encaminhado para o media player.

### Tolerância a problemas do UDP

Uma vantagem desta abordagem é que **cada pacote pode ser decifrado de forma independente**, pois o keystream é gerado a partir da chave e do contador presente no próprio pacote.

Isto é especialmente importante porque a comunicação usa **UDP**, onde podem ocorrer:
- perda de pacotes  
- receção fora de ordem  
- duplicação de pacotes  

Como o keystream não depende de pacotes anteriores, a perda ou reordenação de pacotes **não quebra a sincronização** entre o servidor e o proxy, apenas o frame correspondente ao pacote perdido é afetado.

### Limitações

Esta solução garante **confidencialidade dos dados**, mas por si só **não garante integridade nem autenticação**. Caso fosse necessário, poderia ser adicionada uma **tag HMAC** aos pacotes para permitir verificar a integridade dos dados recebidos.
