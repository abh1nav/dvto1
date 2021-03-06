/**
 * Taken from the storm-starter project on GitHub
 * https://github.com/nathanmarz/storm-starter/ 
 */
package org.dvto.storm;

import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;
import backtype.storm.Config;
import backtype.storm.spout.SpoutOutputCollector;
import backtype.storm.task.TopologyContext;
import backtype.storm.topology.OutputFieldsDeclarer;
import backtype.storm.topology.base.BaseRichSpout;
import backtype.storm.tuple.Fields;
import backtype.storm.tuple.Values;
import backtype.storm.utils.Utils;

import com.google.common.base.Preconditions;

@SuppressWarnings({ "rawtypes", "serial" })
public class TwitterSampleSpout extends BaseRichSpout {
	SpoutOutputCollector _collector;
	LinkedBlockingQueue<Status> queue = null;
	TwitterStream _twitterStream;
	String _username;
	String _pwd;

	public TwitterSampleSpout(String username, String pwd) {
		Preconditions.checkArgument(!username.equals(""));
		Preconditions.checkArgument(!pwd.equals(""));
		_username = username;
		_pwd = pwd;
	}

	@Override
	public void open(Map conf, TopologyContext context, SpoutOutputCollector collector) {
		queue = new LinkedBlockingQueue<Status>(1000);
		_collector = collector;
		
		StatusListener listener = new StatusListener() {
			@Override
			public void onStatus(Status status) {
				queue.offer(status);
			}

			@Override public void onDeletionNotice(StatusDeletionNotice sdn) {}
			@Override public void onTrackLimitationNotice(int i) {}
			@Override public void onScrubGeo(long l, long l1) {}
			@Override public void onException(Exception e) {}
		};
		
		TwitterStreamFactory fact = new TwitterStreamFactory(
				new ConfigurationBuilder().setUser(_username).setPassword(_pwd)
						.build());
		_twitterStream = fact.getInstance();
		_twitterStream.addListener(listener);
		_twitterStream.sample();
	}

	@Override
	public void nextTuple() {
		Status ret = queue.poll();
		
		if (ret == null) {
			Utils.sleep(50);
		}
		else {
			if(ret.isRetweet()) {
				Status retweet = ret.getRetweetedStatus();
				_collector.emit(new Values(retweet.getUser().getScreenName(), retweet.getText(), retweet.getRetweetCount()));
			}
		}
	}

	@Override
	public void close() {
		_twitterStream.shutdown();
	}

	@Override
	public Map<String, Object> getComponentConfiguration() {
		Config ret = new Config();
		ret.setMaxTaskParallelism(1);
		return ret;
	}

	@Override
	public void ack(Object id) {}

	@Override
	public void fail(Object id) {}

	@Override
	public void declareOutputFields(OutputFieldsDeclarer declarer) {
		declarer.declare(new Fields("author", "text", "retweetCount"));
	}

}
