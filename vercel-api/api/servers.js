module.exports = function handler(req, res) {
  // CORS configuration
  res.setHeader('Access-Control-Allow-Credentials', true);
  res.setHeader('Access-Control-Allow-Origin', '*');
  res.setHeader('Access-Control-Allow-Methods', 'GET,OPTIONS');
  res.setHeader(
    'Access-Control-Allow-Headers',
    'X-CSRF-Token, X-Requested-With, Accept, Accept-Version, Content-Length, Content-MD5, Content-Type, Date, X-Api-Version'
  );

  if (req.method === 'OPTIONS') {
    res.status(200).end();
    return;
  }

  // Dynamically extract servers from Environment Variables
  const servers = [];
  
  for (const [key, value] of Object.entries(process.env)) {
    // Only process variables that contain a VPN URI scheme
    if (value && typeof value === 'string' && value.includes('://')) {
      const protocolMatch = value.split('://')[0].toUpperCase();
      
      // Clean up the key to use as the server name (e.g., "SG_SERVER_1" -> "SG SERVER 1")
      const serverName = key.replace(/_/g, ' ');

      servers.push({
        id: key.toLowerCase(),
        name: serverName,
        protocol: protocolMatch,
        config: value
      });
    }
  }

  // If no valid environment variables are found, provide a fallback dummy server
  if (servers.length === 0) {
    servers.push({
      id: 'dummy-1',
      name: 'No Server Found (Add Env Var in Vercel)',
      protocol: 'UNKNOWN',
      config: 'unknown://dummy'
    });
  }

  // Return as JSON for the Android App
  res.status(200).json({
    success: true,
    servers: servers
  });
}
