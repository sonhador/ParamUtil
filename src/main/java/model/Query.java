package model;

import java.util.List;

public class Query {
	public int page;
	public String id;
	public List<Filter> filters;
	public List<Query> queries;
	public Query query;
	public Filter filter;
}
