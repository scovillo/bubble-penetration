export function getCenteredStartRank(userRank, precedingRanks = 10) {
  return Math.max(1, userRank - precedingRanks);
}
