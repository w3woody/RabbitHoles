package com.chaosinmotion.git.test.utils;

import java.util.Objects;

public class Pair<A, B>
{
	public final A first;
	public final B second;

	public Pair(A first, B second)
	{
		this.first = first;
		this.second = second;
	}

	@Override
	public boolean equals(Object o)
	{
		if (this == o) return true;
		if (!(o instanceof Pair<?, ?> pair)) return false;

		if (!Objects.equals(first, pair.first))
			return false;
		return Objects.equals(second, pair.second);
	}

	@Override
	public int hashCode()
	{
		int result = first != null ? first.hashCode() : 0;
		result = 31 * result + (second != null ? second.hashCode() : 0);
		return result;
	}
}
