/**
 * Clean and normalize merchant name strings for comparison
 */
function normalizeString(str) {
  if (!str) return '';
  return str
    .toLowerCase()
    .replace(/[^a-z0-9\s]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim();
}

/**
 * Check if two merchant names have meaningful overlap
 */
function compareMerchants(merchantA, merchantB) {
  const normA = normalizeString(merchantA);
  const normB = normalizeString(merchantB);

  if (!normA || !normB) return 0;
  if (normA === normB) return 30; // Exact match

  if (normA.includes(normB) || normB.includes(normA)) {
    return 20; // Substring match
  }

  const wordsA = normA.split(' ').filter((w) => w.length > 2);
  const wordsB = normB.split(' ').filter((w) => w.length > 2);

  const commonWords = wordsA.filter((w) => wordsB.includes(w));
  if (commonWords.length > 0) {
    return 15; // Keyword overlap
  }

  return 0;
}

/**
 * Compare dates and return score based on day difference
 */
function compareDates(dateStrA, dateStrB) {
  if (!dateStrA || !dateStrB) return 0;
  try {
    const dA = new Date(dateStrA.substring(0, 10));
    const dB = new Date(dateStrB.substring(0, 10));
    if (isNaN(dA.getTime()) || isNaN(dB.getTime())) return 0;

    const diffDays = Math.abs((dA - dB) / (1000 * 60 * 60 * 24));
    if (diffDays === 0) return 30; // Same day
    if (diffDays <= 2) return 20;  // Within 2 days
    if (diffDays <= 5) return 10;  // Within 5 days
    return 0;
  } catch {
    return 0;
  }
}

/**
 * Evaluate matching candidate transactions against an OCR extraction result
 * @param {Object} ocrData
 * @param {Array} transactions
 * @returns {{
 *   status: 'STRONG_MATCH' | 'MULTIPLE_MATCHES' | 'NO_MATCH' | 'UNCERTAIN_OCR',
 *   matchedTransaction?: Object,
 *   candidates?: Array<{ transaction: Object, score: number, reasons: Array<string> }>
 * }}
 */
export function matchReceiptWithTransactions(ocrData, transactions = []) {
  if (!ocrData || ocrData.status !== 'COMPLETED') {
    return { status: 'UNCERTAIN_OCR' };
  }

  const detectedAmount = Number(ocrData.detectedAmount);
  if (!detectedAmount || isNaN(detectedAmount) || detectedAmount <= 0) {
    return { status: 'UNCERTAIN_OCR' };
  }

  const detectedMerchant = ocrData.detectedMerchant || '';
  const detectedDate = ocrData.detectedDate || '';

  const scoredCandidates = [];

  for (const tx of transactions) {
    const txAmount = Number(tx.amount || 0);
    const amountMatches = Math.abs(txAmount - detectedAmount) < 0.01;

    let score = 0;
    const reasons = [];

    if (amountMatches) {
      score += 50;
      reasons.push('Exact amount match');
    }

    const merchantScore = compareMerchants(detectedMerchant, tx.merchant);
    if (merchantScore > 0) {
      score += merchantScore;
      if (merchantScore >= 20) {
        reasons.push('Merchant name matches');
      } else {
        reasons.push('Similar merchant name');
      }
    }

    const dateScore = compareDates(detectedDate, tx.transactionDate);
    if (dateScore > 0) {
      score += dateScore;
      if (dateScore >= 20) {
        reasons.push('Date matches within 2 days');
      } else {
        reasons.push('Date is close');
      }
    }

    // Only consider candidate if at least amount matches or (merchant & date strongly match)
    if (score >= 40) {
      scoredCandidates.push({
        transaction: tx,
        score,
        reasons,
      });
    }
  }

  // Sort candidates by highest match score descending
  scoredCandidates.sort((a, b) => b.score - a.score);

  if (scoredCandidates.length === 0) {
    return { status: 'NO_MATCH' };
  }

  // High-confidence strong threshold: Score >= 70 (Amount match + Merchant or Date match)
  const strongMatches = scoredCandidates.filter((c) => c.score >= 70);

  if (strongMatches.length === 1) {
    // Single unambiguous strong match!
    return {
      status: 'STRONG_MATCH',
      matchedTransaction: strongMatches[0].transaction,
      reasons: strongMatches[0].reasons,
    };
  }

  if (scoredCandidates.length > 1) {
    return {
      status: 'MULTIPLE_MATCHES',
      candidates: scoredCandidates.slice(0, 5), // top 5 plausible matches
    };
  }

  if (strongMatches.length === 0 && scoredCandidates.length === 1 && scoredCandidates[0].score >= 50) {
    // Single match with amount only: show user choice rather than blind guess
    return {
      status: 'MULTIPLE_MATCHES',
      candidates: scoredCandidates,
    };
  }

  return { status: 'NO_MATCH' };
}

export default matchReceiptWithTransactions;
