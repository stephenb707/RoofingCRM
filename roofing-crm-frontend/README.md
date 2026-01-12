# Roofing CRM Frontend

A Next.js web application for the Roofing CRM system.

## Tech Stack

- **Next.js 14** (App Router)
- **TypeScript**
- **Tailwind CSS**
- **React Query** (@tanstack/react-query)
- **Axios**

## Getting Started

### Prerequisites

- Node.js 18+ 
- npm or yarn
- Backend running at http://localhost:8080

### Installation

```bash
cd roofing-crm-frontend
npm install
```

### Environment Variables

Create a `.env.local` file in this directory:

```env
NEXT_PUBLIC_API_BASE_URL=http://localhost:8080
```

### Development

```bash
npm run dev
```

Open [http://localhost:3000](http://localhost:3000) in your browser.

### Build

```bash
npm run build
npm start
```

## Project Structure

```
roofing-crm-frontend/
├── app/                    # Next.js App Router pages
│   ├── layout.tsx          # Root layout with providers
│   ├── page.tsx            # Home (redirects to login)
│   ├── login/              # Login page
│   ├── select-tenant/      # Tenant selection page
│   └── app/                # Protected area
│       ├── layout.tsx      # Protected layout with header/nav
│       └── customers/      # Customers page
├── lib/                    # Shared utilities
│   ├── types.ts            # TypeScript types
│   ├── authState.ts        # Auth state persistence
│   ├── apiClient.ts        # Axios client with interceptors
│   ├── AuthContext.tsx     # Auth context provider
│   └── ReactQueryProvider.tsx
├── public/                 # Static assets
└── ...config files
```

## Authentication Flow

1. User visits `/login` and enters credentials
2. On successful login, JWT token and tenant list are stored
3. If multiple tenants, user selects one at `/select-tenant`
4. User is redirected to `/app/customers`
5. All API calls include:
   - `Authorization: Bearer <token>`
   - `X-Tenant-Id: <selected tenant UUID>`

## Backend CORS

Make sure the backend allows CORS from `http://localhost:3000`. Add this to your Spring Security config if not already present.
