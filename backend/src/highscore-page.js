export function getCenteredStartRank(userRank, precedingRanks = 20) {
  return Math.max(1, userRank - precedingRanks);
}
