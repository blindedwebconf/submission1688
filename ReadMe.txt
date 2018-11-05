How to build:
Import the project into eclipse.
	Click "File" -> "Import" 
	Select "General" -> "Existing Project into Workspace" -> "Next"
	Select "Select root Directory" -> "Browse" Select the project -> "Finish"
Add the four libraries from the lirbaries folder to the project. (apache-commons-io, apache-jena, Guava, HDT)
Now build a runnable jar:
	Click "File" -> "Export" -> "Java" -> "Runnable JAR file"
	Select Main - Jena and a destination to export to. Select "Extract required libraries into generated JAR" and press "Finish"
If the Main does not show up to be selected when trying to export as a runnable jar:
	Right click the Main class in protocol, click "Run as" -> "Java Application"
A functioning runnable jar can be found in the git repo.

Key/TrustStore
In order to run the protocol a keystore/truststore is needed. This is needed for the secure communication using TLS.
On how to create such a store see:
https://docs.oracle.com/javase/tutorial/security/toolsign/step3.html
Usually the Buyer and Seller are run on different machines. 
Then both need their own store and they have to trust each other, meaning a certificate needs to be shared. 
If for some reason both run on the same machine (maybe for testing purposes) they both can use the same store which will automatically trust itself.
On how to extract a certificate and how to add it to a different keystore see:
https://docs.oracle.com/cd/E54932_01/doc.705/e54936/cssg_create_ssl_cert.htm#CSVSG178
A trivial way to get two keystores which trust each other is to create one as seen above and then send it to the second machine.

How to run the protocol:
Run the protocol through a commandline with:
java -jar Protocol.jar
You may need to alow the program more memory and a bigger stack size depending on the size of the graph e.g.
java -Xmx50G -Xss16m -jar Protocol.jar
This needs to be run twice, once for the Seller and once for the Buyer. 

How to start as Seller:
Once the protocol is startet you get asked for some input.
"To run the protocol as Seller enter 'seller'. To run the protocol as Buyer enter 'buyer'."
Enter: seller
"Enter path to Log File."
Example entry: /home/XXX/logs/seller.log
"Enter path to the Knowledge Graph."
Example entry: /home/XXX/datasets/SellerKnowledgeGraph.ttl
The entered file needs to be of type: .ttl, .rdf, .nt, .owl, .jsonld, .n3 or .hdt
If it is not a .hdt file you will be asked for a folder containing a TDB Jena index of the knowledge graph. If non exists yet an index will be created ini the folder you enter.
"Enter path to the index of the Knowledge Graph."
Example entry: /home/XXX/datasets/SellerKnowledgeGraph
"Enter path to your Keystore."
Enter the path to the keystore you created previously.
Example entry: /home/XXX/KeyStore
"Enter Keystore password."
Example entry: password

Now the Seller instance waits until the Buyer connects.

How to start as Buyer:
Once the protocol is startet you get asked for some input.
"To run the protocol as Seller enter 'seller'. To run the protocol as Buyer enter 'buyer'."
Enter: buyer
"Enter path to Log File."
Example entry: /home/XXX/logs/buyer.log
"Enter path to the Knowledge Graph."
Example entry: /home/XXX/datasets/BuyerKnowledgeGraph.ttl
The entered file needs to be of type: .ttl, .rdf, .nt, .owl, .jsonld, .n3 or .hdt
If it is not a .hdt file you will be asked for a folder containing a TDB Jena index of the knowledge graph. If non exists yet an index will be created ini the folder you enter.
"Enter path to the index of the Knowledge Graph."
Example entry: /home/XXX/datasets/BuyerKnowledgeGraph
"Enter path to your Truststore."
Enter the path to the keystore you created previously.
Example entry: /home/XXX/KeyStore
"Enter Keystore password."
Example entry: password
"Enter IP address to connect to."
Enter the IP of the machine the Seller runs on. If both run on the same machine you can use 127.0.0.1
Note the Seller needs to enter his Keystore and password before the Buyer does. Otherweise the Buyer tries to connect to a not yet existing server fails and terminates.

Exampletory run of the rest of the protocol:
Naturally from here on entries of both parties need to match.
Now both parties get asked if they would like to calculate the entropies: "Calculate Entropies. [yes, no]"
Both enter "yes" or "no".
Both get asked to enter which entropies shall be computed:
enter numbers from 1 to 9 individually or enter "all".
enter "done" when all entropies that you whish to compute are entered.
Both get asked if the intersection is to be calculated: "Calculate the intersection. [yes, no]"
Both enter "yes" or "no".
	If the intersection gets calculated the Seller gets asked to enter a Bloom filter false positive probability:
	"Enter wished false positive probability for Bloom filter. Must be 0<fpp<=1."
	Seller enters e.g. 0.03 (for 3% fpp)
	The Buyer gets asked to enter a folder where the intersection should be written to. If the data quality step also is run this folder will be used again to write the model parts to. "Enter the path to a folder where Models should be written to."
	Buyer enters e.g. /home/XXX/folder
Both get asked if the protocol should continue: "Would you like to keep going with the protocol? [yes, no]"
Both enter "yes" or "no".
Both get asked if the protocol should continue: "Would you like to keep going with the protocol? [yes, no]"
Both enter "yes" or "no".
Both get asked if the statistics should be run: "Calculate descriptive Statistics. [yes, no]"
Both enter "yes" or "no".
Both get asked if the protocol should continue: "Would you like to keep going with the protocol? [yes, no]"
Both enter "yes" or "no".
Bot get asked if the data quality step should be run: "Run Oblivious Transfer. [yes, no]"
Both enter "yes" or "no".
	If "yes" was entered the Seller is asked to enter a knowledge graph partitioning strategy: "Enter a partitioning strategy. 'balancedDBSCAN', 'DBSCAN', 'range', 'resource'"
	Example entry: balancedDBSCAN
	Depending on the strategy he is asked additional information.
	Both get asked to enter how many parts should be shared by the oblivious transfer: "Enter number of model parts to be shared via oblivious transfer. Must be an integer <= 1."
	Example entry: 5
	If the intersection step has not been run the Buyer is asked to enter a folder where the received parts are to be written to. "Enter the path to a folder where Models should be written to."
	Example entry: /home/XXX/folder
Both get asked if the protocol should continue: "Would you like to keep going with the protocol? [yes, no]"
Both enter "yes" or "no".
At this point the Seller is done.
In the verification step the Buyer may be asked to enter a false positive probability for the intersection Bloom filter. Be sure to enter the same value as the Seller was supposed to.
Also the Buyer will be asked to enter a partitioning strategy. Make sure to enter the strategy the Seller was supposed to use during the data quality step.
Now the Buyer is done as well.

For testing purposes the protocol can be run with the argument "complete":
java -Xmx50G -Xss16m -jar Protocol.jar complete
This causes all steps to be run with default values. However "seller" or "buyer" still needs to be entered as well as the paths to the logfile, knowledge graph and keystore.
As default IP 127.0.0.1 is used (both instances run on the same machine).
Default fpp for Bloom filter: 0.03
Default partitioning strategy: balancedDBSCAN
Default number of secrets to be shared: 5
All steps are run and all entropies calculated. 
These values can be changed in protocol.GetUserInput.

The no privacy version which was used for the baseline evaluation can be started in the same way as the usual protocol.
The only difference is: instead of "seller"/"buyer" enter "sellerNP" and "buyerNP" when asked.