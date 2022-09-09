// Copyright 2022 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.bazel;

import static com.google.common.collect.Comparators.lexicographical;
import static com.google.common.primitives.Booleans.falseFirst;
import static com.google.common.primitives.Booleans.trueFirst;
import static java.util.Comparator.comparing;

import com.google.auto.value.AutoValue;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.devtools.build.lib.bazel.bzlmod.Version.ParseException;
import java.util.Comparator;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Nullable;

@AutoValue
public abstract class BazelVersion implements Comparable<BazelVersion>{

  private static final Pattern PATTERN =
      Pattern.compile("(?<release>(?:\\d+\\.)*\\d+)(?<suffix>(?:(.*)))?");

  /** Represents "empty string" version, which compares higher than everything else */
  public static final BazelVersion EMPTY =
      new AutoValue_BazelVersion(ImmutableList.of(), "", "");

  /** Returns the "release" part of the version string as a list of integers. */
  abstract ImmutableList<Integer> getRelease();

  /** Returns the "suffix" part of the version that starts after the integers */
  abstract String getSuffix();

  /** Returns the original version string. */
  public abstract String getOriginal();

  /** Whether this is just the "empty string" version */
  boolean isEmpty() {
    return getOriginal().isEmpty();
  }

  /** Whether this is a prerelease or a release candidate */
  boolean isPrereleaseOrCandidate() {
    String suffix = getSuffix();
    return !Strings.isNullOrEmpty(suffix) &&
        (suffix.startsWith("-pre") || suffix.startsWith("rc"));
  }

  /** Parses a version string into a {@link BazelVersion} object. */
  public static BazelVersion parse(String version) throws ParseException {
    if (version.isEmpty()) {
      return BazelVersion.EMPTY;
    }
    Matcher matcher = PATTERN.matcher(version);
    if (!matcher.matches()) {
      throw new ParseException("bad version (does not match regex): " + version);
    }

    String release = matcher.group("release");
    @Nullable String suffix = matcher.group("suffix");
    suffix = !Strings.isNullOrEmpty(suffix)? suffix : "";

    ImmutableList.Builder<Integer> releaseSplit = new ImmutableList.Builder<>();
    for (String number : release.split("\\.")) {
      try {
        releaseSplit.add(Integer.valueOf(number));
      } catch (NumberFormatException e) {
        throw new ParseException("error parsing version: " + version, e);
      }
    }

    return new AutoValue_BazelVersion(releaseSplit.build(), suffix, version);
  }

  private static final Comparator<BazelVersion> COMPARATOR =
      comparing(BazelVersion::isEmpty, falseFirst())
          .thenComparing(BazelVersion::getRelease, lexicographical(Comparator.<Integer>naturalOrder()))
          .thenComparing(BazelVersion::isPrereleaseOrCandidate, trueFirst());
          /*
            If this comparator is used to compare two Bazel Versions, will need to update
            suffix in regex and add comparing for -pre and rc parts
           */

  @Override
  public int compareTo(BazelVersion o) {
    return Objects.compare(this, o, COMPARATOR);
  }

}
