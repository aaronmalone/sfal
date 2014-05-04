package edu.osu.sfal.util;

import com.couchbase.client.CouchbaseClient;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class CouchbaseMain {
	public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
		List<URI> nodes = new ArrayList<>();
		nodes.add(URI.create("http://127.0.0.1:8091/pools"));

		CouchbaseClient client = new CouchbaseClient(nodes, "default", "");
		client.set("hello", "you say goodbye and i say hello. hello hello!").get();

		String result = (String) client.get("hello");
		System.out.println("result: " + result);

		client.getView("", "");

		client.shutdown();
	}
}
