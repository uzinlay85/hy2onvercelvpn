export default function handler(req, res) {
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

  // Extract keys from Environment Variables
  // In Vercel, you'll need to set these in the Project Settings -> Environment Variables
  const hysteria2Key = process.env.HYSTERIA2_KEY || 'hysteria2://dummy_key?sni=example.com';
  const vlessKey = process.env.VLESS_KEY || 'vless://dummy_key@example.com:443?encryption=none&security=tls';
  const outlineKey = process.env.OUTLINE_KEY || 'ss://dummy_key@example.com:443#Outline';

  // Return as JSON for the Android App
  res.status(200).json({
    success: true,
    servers: [
      {
        id: 'hysteria2-1',
        name: 'Hysteria2 Free Server',
        protocol: 'HYSTERIA2',
        config: hysteria2Key
      },
      {
        id: 'vless-1',
        name: 'VLESS Free Server',
        protocol: 'VLESS',
        config: vlessKey
      },
      {
        id: 'outline-1',
        name: 'Outline Free Server',
        protocol: 'OUTLINE',
        config: outlineKey
      }
    ]
  });
}
