DailyBot
========

Forex signal provider for Zulutrade. This same system (with the only addition of an AI "filter" written in Octave) is currently controlling the Zulutrade signal provider DailyBot SSI - EURO: http://www.zulutrade.com/trader/123804

The system consist of several packages:

The control package contain several utilities classes, some of which perform functions available in already existing libraries but which were written to learn more about the Java API. Among them there is a fine thread monitor system (DailyThread, DailyThreadAdmin and DailyThreadInfo classes) which was written mainly to assist in the solution of hard to debug deadlocks, and the DailyLog, which is a log that transmit log information via email (GMail) and chat (GTalk).

The connection packages contain classes that help in the connection with DailyFX, Zulutrade, MySql, GTalk and GMail. The class XMLPersistentObject defines an object which creates its own table in the database and can be stored and retrieved automatically as a XML document from the database. The ChatConnection class allows the system administrator to control and monitor the system via chat commands.

The model packages contain the basic model of DailyBot: a set of StrategySystems own and update several Strategies, which relay some StrategySignals to SignalProviders, which in turn execute them in Brokers, provided their Filters approve them.
The current system implements the DailyFXStrategySystem, which relay DailyFX signals to a Zulutrade enabled signal provider, which uses an AI filter written in Octave (to learn more about DailyFX you can check this page: http://www.dailyfx.com/, about Zulutrade: http://www.zulutrade.com/ and about the zulutrade API (which is used in this project): https://www.zulutrade.com/restapi-reference).

The view package contain several UIs which can be used to control the system and assist in the process of determining which Strategies should be active for which Forex Pairs. The connection between the UI client and server is done through Java Remote Method Invocation (Java RMI), so that the system can be monitored remotely.

About DailyBot:

I started this project in 2010 with the purpose of relaying DailyFX's free signals to Zulutrade and also to learn more about the Java API. The first approach in order to obtain the signals from DailyFX's page was taking screenshots of the page and doing OCR. Then, after trying a lot, I was able to login directly from Java and receive the signals programmatically from a page that transmitted them as JSON objects.

After that problem was solved, the new problem was choosing which signals to relay, since they were not all of the same quality. First of all it was important to define several characteristics to distinguish between the market conditions in which each signal was created, so the chosen characteristics were: Daily FX's Speculative Sentiment Index (SSI), the Standard and Poors (SP500) Volatility Index (VIX), the Average True Range of the forex pair (ATR) and the Relative Strength Index of the forex pair (RSI). To learn about them:
SSI: http://www.dailyfx.com/forex/education/trading_tips/chart_of_the_day/2013/06/10/How_to_Read_SSI.html
VIX: http://en.wikipedia.org/wiki/VIX
ATR: http://en.wikipedia.org/wiki/Average_true_range
RSI: http://en.wikipedia.org/wiki/Relative_strength_index

The first filter was done manually, so for the first version I manually choose for which ranges of the characteristics each Forex strategy and currency pair would be active (for example, strategy BREAKOUT2 on EURO-USD would only be active when the VIX was between 25 and 45). Then, with the help of a friend (https://github.com/sebasutp), we created an AI system which uses probabilistic models to determine for a particular market condition what is the probability of a trade to be successful and what is the expected profit. That AI system, which is written in Octave, is the only part of DailyBot that is not public or released here in GitHub.

How to setup:

The project is an Eclipse project. If you correctly fill all the DailyBot.conf properties, it will work without major problems (you might have to correct some permission problems with java.policy and the RMI codebase). In order to use RMI you should first start the rmiregistry. 
About the database: the system automatically creates the tables (it currently only use 4 tables), so you only need to provide the address and login data for an empty MySql database (see the DailyBot.conf file for details).

Since the system was developed mostly by a single person, I didn't use Javadoc nor commented the code, however I plan to do it in a future version.
